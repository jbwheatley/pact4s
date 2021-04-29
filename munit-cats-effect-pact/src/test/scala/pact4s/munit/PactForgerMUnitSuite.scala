package pact4s.munit

import au.com.dius.pact.consumer.{ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import org.http4s.{Header, Headers, Method, Request, Uri}
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIString

class PactForgerMUnitSuite extends PactForger {

  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./munit-cats-effect-pact/target/pacts"
  )

  def pact: RequestResponsePact =
    ConsumerPactBuilder
      .consumer("Consumer")
      .hasPactWith("Provider")
      .uponReceiving("a request to say Hello")
      .path("/hello")
      .method("POST")
      .body("{\"name\": \"harry\"}")
      .willRespondWith()
      .status(200)
      .body("{\"hello\": \"harry\"}")
      .uponReceiving("a request to say Goodbye")
      .path("/goodbye")
      .method("GET")
      .willRespondWith()
      .status(204)
      .toPact()

  val client = ResourceSuiteLocalFixture(
    "httpClient",
    EmberClientBuilder.default[IO].build
  )

  override def additionalMunitFixtures: Seq[Fixture[_]] = Seq(client)

  pactTest("munit pact test") { server =>
    val request = Request[IO](method = Method.POST,
                              uri = Uri.unsafeFromString(server.getUrl + "/hello"),
                              headers = Headers(Header.Raw(CIString("content-type"), "application/json")))
      .withEntity("{\"name\": \"harry\"}")
    client().run(request).use {
      _.as[String].assertEquals("{\"hello\": \"harry\"}")
    }
  }

  pactTest("another munit pact test") { server =>
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/goodbye"),
                              headers = Headers(Header.Raw(CIString("content-type"), "application/json")))
    client().run(request).use {
      _.status.code.pure[IO].assertEquals(204)
    }
  }
}
