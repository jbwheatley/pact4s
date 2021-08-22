package pact4s

import cats.effect.kernel.Ref
import cats.effect.{IO, Resource}
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
import pact4s.Authentication.BasicAuth
import pact4s.PactSource.{FileSource, PactBrokerWithSelectors}

import java.io.File

class MockProviderServer(port: Int) {
  def server: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(port).get)
      .withHttpApp(app)
      .build

  private implicit val entityDecoder: EntityDecoder[IO, ProviderState] = {
    implicit val decoder: Decoder[ProviderState] = Decoder.forProduct1("state")(ProviderState)
    jsonOf
  }

  private val stateRef: Ref[IO, Option[String]] = Ref.unsafe(None)

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
            case Some(s) => Ok(Json.obj("found" -> s.asJson))
            case None    => NotFound()
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
          req.as[ProviderState].flatMap {
            case ProviderState("bob exists") =>
              stateRef.set(Some("bob")) *> Ok()
            case _ => Ok()
          }
      }
      .orNotFound

  private def requestFilter(request: ProviderRequest): List[ProviderRequestFilter] =
    request.uri.getPath match {
      case s if s.matches(".*/authorized") => List(ProviderRequestFilter.SetHeaders(("Authorization", "Bearer token")))
      case _                               => Nil
    }

  def fileSourceProviderInfo(
      consumerName: String,
      providerName: String,
      fileName: String,
      verificationSettings: Option[VerificationSettings] = None
  ): ProviderInfoBuilder =
    ProviderInfoBuilder(
      name = providerName,
      pactSource = FileSource(Map(consumerName -> new File(fileName)))
    ).withPort(port)
      .withOptionalVerificationSettings(verificationSettings)
      .withStateChangeEndpoint("setup")
      .withRequestFilter(requestFilter)

  def brokerProviderInfo(
      providerName: String,
      verificationSettings: Option[VerificationSettings] = None
  ): ProviderInfoBuilder =
    ProviderInfoBuilder(
      name = providerName,
      pactSource = PactBrokerWithSelectors(
        brokerUrl = "https://test.pact.dius.com.au"
      ).withAuth(BasicAuth("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1"))
        .withPendingPacts(enabled = true)
        .withProviderTags("SNAPSHOT")
        .withSelectors(ConsumerVersionSelector())
    ).withPort(port)
      .withOptionalVerificationSettings(verificationSettings)
      .withStateChangeEndpoint("setup")
      .withRequestFilter(requestFilter)
}

private[pact4s] final case class Name(name: String)

private[pact4s] object Name {
  implicit val decoder: Decoder[Name]                 = Decoder.forProduct1("name")(Name(_))
  implicit val entityDecoder: EntityDecoder[IO, Name] = jsonOf
}
