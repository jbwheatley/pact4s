package pact4s.munit.requestresponse

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import io.circe.Json
import io.circe.syntax.EncoderOps
import munit.AnyFixture
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.{CIString, CIStringSyntax}
import pact4s.TestModels
import pact4s.munit.RequestResponsePactForger

class PactForgerSuite extends RequestResponsePactForger {

  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./munit-cats-effect-pact/target/pacts"
  )

  val pact: RequestResponsePact = TestModels.requestResponsePact

  val client = ResourceSuiteLocalFixture(
    "httpClient",
    EmberClientBuilder.default[IO].build
  )

  override def additionalMunitFixtures: Seq[AnyFixture[_]] = Seq(client)

  pactTest("munit pact test") { server =>
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(server.getUrl + "/hello"),
      headers = Headers(Header.Raw(CIString("other-header"), "howdy"))
    )
      .withEntity(Json.obj("name" -> "harry".asJson))
    client().run(request).use {
      _.as[String].assertEquals("{\"hello\":\"harry\"}")
    }
  }

  pactTest("another munit pact test") { server =>
    val request = Request[IO](
      uri = Uri.unsafeFromString(server.getUrl + "/goodbye"),
      headers = Headers(`Content-Type`(MediaType.application.json))
    )
    client().run(request).use {
      _.status.code.pure[IO].assertEquals(204)
    }
  }

  pactTest("test with provider state") { server =>
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/anyone-there/bob"))
    client().run(request).use {
      _.as[String].assertEquals("{\"found\":\"bob\"}")
    }
  }

  pactTest("test with provider state with no params") { server =>
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/anyone-there"))
    client().run(request).use {
      _.status.code.pure[IO].assertEquals(404)
    }
  }

  pactTest("test with generated auth header") { server =>
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/authorized"))
      .putHeaders(headers.Authorization(Credentials.Token(ci"Bearer", "super-secure")))
    client()
      .run(request)
      .use {
        _.status.code.pure[IO].assertEquals(200)
      }
  }
}
