package pact4s.scalatest

import au.com.dius.pact.consumer.{ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.IO
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import org.http4s.{Header, Headers, Method, Request, Uri}
import org.typelevel.ci.CIString

class TestSuite extends AnyFlatSpec with Matchers with PactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./scalatest-pact/target/pacts"
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

  val client = EmberClientBuilder.default[IO].build.allocated.unsafeRunSync()._1

  it should "scalatest pact test" in {
    val request = Request[IO](method = Method.POST,
                              uri = Uri.unsafeFromString(server.getUrl + "/hello"),
                              headers = Headers(Header.Raw(CIString("content-type"), "application/json")))
      .withEntity("{\"name\": \"harry\"}")
    client
      .run(request)
      .use {
        _.as[String].map(_ shouldBe "{\"hello\": \"harry\"}")
      }
      .unsafeRunSync()
  }

  it should "another scalatest pact test" in {
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/goodbye"),
                              headers = Headers(Header.Raw(CIString("content-type"), "application/json")))
    client
      .run(request)
      .use {
        _.status.code.pure[IO].map(_ shouldBe 204)
      }
      .unsafeRunSync()
  }
}
