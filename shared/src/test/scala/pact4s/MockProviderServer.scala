package pact4s

import java.io.File
import au.com.dius.pact.provider.ProviderInfo
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

  def fileSourceProviderInfo: ProviderInfoBuilder =
    ProviderInfoBuilder(
      "Pact4sProvider",
      "http",
      "localhost",
      port,
      "/",
      publishResults = false,
      FileSource("Pact4sConsumer", new File("./scripts/Pact4sConsumer-Pact4sProvider.json"))
    )

  def brokerProviderInfo: ProviderInfoBuilder =
    ProviderInfoBuilder(
      "Pact4sProvider",
      "http",
      "localhost",
      port,
      "/",
      publishResults = true,
      PactBrokerWithSelectors(
        "https://test.pact.dius.com.au",
        Some(BasicAuth("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1")),
        enablePending = true,
        None,
        Nil,
        ConsumerVersionSelector(None, latest = true) :: Nil
      )
    )
}

private[pact4s] final case class Name(name: String)

private[pact4s] object Name {
  implicit val decoder: Decoder[Name]                 = Decoder.forProduct1("name")(Name(_))
  implicit val entityDecoder: EntityDecoder[IO, Name] = jsonOf
}
