/*
 * Copyright 2021 io.github.jbwheatley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pact4s

import cats.data.Kleisli
import cats.effect.kernel.{Deferred, Ref}
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.implicits._
import com.comcast.ip4s.{Host, Port}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Json}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.jsonOf
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Server
import pact4s.circe.implicits._
import pact4s.provider.Authentication.TokenAuth
import pact4s.provider.PactSource.{FileSource, PactBrokerWithSelectors}
import pact4s.provider._
import sourcecode.{File => SCFile}

import java.io.File
import java.net.URI
import scala.concurrent.duration.DurationInt
import scala.util.chaining.scalaUtilChainingOps

class MockProviderServer(port: Int, hasFeatureX: Boolean = false, enableRequestLogging: Boolean = false)(implicit
    file: SCFile
) {

  val featureXState: Deferred[IO, Boolean] = Deferred.unsafe

  def server: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(port).get)
      .withHttpApp(middleware(app))
      .withShutdownTimeout(0.seconds)
      .build

  private implicit val entityDecoder: EntityDecoder[IO, ProviderState] = jsonOf

  private[pact4s] val stateRef: Ref[IO, List[String]] = Ref.unsafe(Nil)

  private[pact4s] val stateChangeFunction: ProviderState => IO[Unit] = {
    case ProviderState("bob exists", params) =>
      val person: String = params("person")
      stateRef.set(List(person))
    case ProviderState("nobody is home", _) =>
      stateRef.set(Nil)
    case _ => IO.unit
  }

  private def middleware: HttpApp[IO] => HttpApp[IO] = { app =>
    Kleisli { (req: Request[IO]) =>
      app(req).timed.flatMap { case (time, resp) =>
        IO.println(
          Console.BLUE +
            s"[PACT4S TEST INFO] Request to mock provider server with port $port in test suite ${file.value.split("/").takeRight(3).mkString("/")}" +
            s"\n[PACT4S TEST INFO] Request(method=${req.method}, uri=${req.uri}, headers=${req.headers})\n[PACT4S TEST INFO] ${resp
                .toString()}\n[PACT4S TEST INFO] Duration: ${time.toMillis} millis" +
            Console.WHITE
        ).whenA(enableRequestLogging)
          .as(resp)
      }
    }
  }

  private def app: HttpApp[IO] =
    HttpRoutes
      .of[IO] {
        case GET -> Root / "goodbye" =>
          NoContent()
        case req @ POST -> Root / "hello" =>
          req.as[Name].flatMap {
            case Name("harry") => Ok(Json.obj("hello" -> "harry".asJson))
            case _             => NotFound()
          }
        case GET -> Root / "anyone-there" =>
          stateRef.get.flatMap {
            case Nil   => NotFound()
            case other => Ok(Json.obj("found" -> other.mkString(",").asJson))
          }
        case GET -> Root / "anyone-there" / person =>
          stateRef.get.flatMap {
            case ss if ss.contains(person) => Ok(Json.obj("found" -> person.asJson))
            case _                         => NotFound()
          }
        case req @ GET -> Root / "authorized" =>
          req.headers
            .get[headers.Authorization]
            .map(_.credentials)
            .flatMap {
              case Credentials.Token(AuthScheme.Bearer, token) => Some(token)
              case _                                           => None
            }
            .map { token =>
              if (token == "token") Ok()
              else Forbidden()
            }
            .getOrElse(Unauthorized(`WWW-Authenticate`(Challenge(AuthScheme.Bearer.toString, "Authorized endpoints."))))
        case req @ POST -> Root / "setup" =>
          req.as[ProviderState].flatMap { state =>
            stateChangeFunction(state) *> Ok()
          }
        case GET -> Root / "feature-x" if hasFeatureX =>
          featureXState.complete(true) *>
            NoContent()
      }
      .orNotFound

  private def requestFilter(request: ProviderRequest): ProviderRequestFilter =
    request.uri.getPath match {
      case s if s.matches(".*/authorized") => ProviderRequestFilter.SetHeaders(("Authorization", "Bearer token"))
      case _                               => ProviderRequestFilter.NoOpFilter
    }

  def fileSourceMessageProviderInfo: ProviderInfoBuilder = fileSourceProviderInfo(
    consumerName = "Pact4sMessageConsumer",
    providerName = "Pact4sMessageProvider",
    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json",
    isHttpPact = false
  )

  def fileSourceProviderInfo(
      consumerName: String = "Pact4sConsumer",
      providerName: String = "Pact4sProvider",
      fileName: String = "./scripts/Pact4sConsumer-Pact4sProvider.json",
      useStateChangeFunction: Boolean = false,
      stateChangePortOverride: Option[Int] = None,
      verificationSettings: Option[VerificationSettings] = None,
      isHttpPact: Boolean = true
  ): ProviderInfoBuilder = {
    val baseBuilder =
      ProviderInfoBuilder(
        name = providerName,
        providerUrl = new URI("http://localhost:0/").toURL,
        pactSource = FileSource(Map(consumerName -> new File(fileName)))
      ).withPort(port)
        .withOptionalVerificationSettings(verificationSettings)
        .withRequestFiltering(requestFilter)

    if (isHttpPact) {
      if (useStateChangeFunction) {
        baseBuilder
          .withStateChangeFunction(state => stateChangeFunction(state).unsafeRunSync())
          .withStateChangeFunctionConfigOverrides(_.withOverrides(portOverride = stateChangePortOverride.getOrElse(0)))
      } else baseBuilder.withStateChangeEndpoint("setup")
    } else baseBuilder

  }

  def brokerMessageProviderInfo: ProviderInfoBuilder =
    brokerProviderInfo(providerName = "Pact4sMessageProvider", isHttpPact = false)
  def brokerProviderInfo(
      providerName: String = "Pact4sProvider",
      verificationSettings: Option[VerificationSettings] = None,
      consumerVersionSelector: ConsumerVersionSelectors = ConsumerVersionSelectors().latestTag("pact4s-test"),
      pendingPactsEnabled: Boolean = false,
      isHttpPact: Boolean = true
  ): ProviderInfoBuilder = {
    val baseBuilder =
      ProviderInfoBuilder(
        name = providerName,
        pactSource = PactBrokerWithSelectors(
          brokerUrl = "https://test.pactflow.io"
        ).pipe(b => if (pendingPactsEnabled) b.withPendingPactsEnabled else b.withPendingPactsDisabled)
          .withAuth(TokenAuth("129cCdfCWhMzcC9pFwb4bw"))
          .withConsumerVersionSelectors(consumerVersionSelector)
      ).withPort(port)
        .withOptionalVerificationSettings(verificationSettings)
        .withRequestFiltering(requestFilter)

    if (isHttpPact) baseBuilder.withStateChangeEndpoint("setup") else baseBuilder
  }
}

private[pact4s] final case class Name(name: String)

private[pact4s] object Name {
  implicit val decoder: Decoder[Name]                 = Decoder.forProduct1("name")(Name(_))
  implicit val entityDecoder: EntityDecoder[IO, Name] = jsonOf
}
