package pact4s

import java.io.File

import au.com.dius.pact.core.model.FileSource
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

  def providerInfo: ProviderInfo = {
    val prov = new ProviderInfo("Provider", "http", "localhost", port.toString(), "/")
    prov.hasPactWith("Consumer", { consumer =>
      consumer.setPactSource(new FileSource(new File("./shared/src/test/resources/Consumer-Provider.json")))
      kotlin.Unit.INSTANCE
    })
    prov
  }
}

private[pact4s] final case class Name(name: String)

private[pact4s] object Name {
  implicit val decoder: Decoder[Name]                 = Decoder.forProduct1("name")(Name(_))
  implicit val entityDecoder: EntityDecoder[IO, Name] = jsonOf
}
