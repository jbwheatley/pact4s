package pact4s

import java.io.File
import cats.effect.{IO, Resource}
import com.comcast.ip4s.{Host, Port}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Json}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.jsonOf
import org.http4s.dsl.io._
import org.http4s.{EntityDecoder, HttpApp, HttpRoutes}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import Name._
import org.http4s.server.Server
import pact4s.Authentication.BasicAuth
import pact4s.PactSource.{FileSource, PactBrokerWithSelectors}

class MockProviderServer(port: Int) {
  def server: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(port).get)
      .withHttpApp(app)
      .build

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
      }
      .orNotFound

  def fileSourceProviderInfo(
      consumerName: String,
      providerName: String,
      fileName: String,
      verificationSettings: Option[VerificationSettings] = None
  ): ProviderInfoBuilder =
    ProviderInfoBuilder(
      name = providerName,
      pactSource = FileSource(consumerName, new File(fileName))
    ).withPort(port).withOptionalVerificationSettings(verificationSettings)

  def brokerProviderInfo(
      providerName: String,
      verificationSettings: Option[VerificationSettings] = None
  ): ProviderInfoBuilder =
    ProviderInfoBuilder(
      name = providerName,
      pactSource = PactBrokerWithSelectors(
        brokerUrl = "https://test.pact.dius.com.au",
        auth = Some(BasicAuth("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1")),
        enablePending = true,
        includeWipPactsSince = None,
        providerTags = Nil,
        selectors = ConsumerVersionSelector(None) :: Nil
      )
    ).withPort(port).withOptionalVerificationSettings(verificationSettings)
}

private[pact4s] final case class Name(name: String)

private[pact4s] object Name {
  implicit val decoder: Decoder[Name]                 = Decoder.forProduct1("name")(Name(_))
  implicit val entityDecoder: EntityDecoder[IO, Name] = jsonOf
}
