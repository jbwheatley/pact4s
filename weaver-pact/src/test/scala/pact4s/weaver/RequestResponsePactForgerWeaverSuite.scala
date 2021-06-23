package pact4s.weaver

import au.com.dius.pact.consumer.{ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.circe.jsonEncoder
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.{Header, Headers, MediaType, Method, Request, Uri}
import org.typelevel.ci.CIString
import weaver.IOSuite
import pact4s.circe.implicits._

object RequestResponsePactForgerWeaverSuite extends IOSuite with RequestResponsePactForger[IO] {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./weaver-pact/target/pacts"
  )

  val pact: RequestResponsePact =
    ConsumerPactBuilder
      .consumer("Consumer")
      .hasPactWith("Provider")
      .uponReceiving("a request to say Hello")
      .path("/hello")
      .method("POST")
      .body(Json.obj("name" -> "harry".asJson), "application/json")
      .headers("other-header" -> "howdy")
      .willRespondWith()
      .status(200)
      .body(Json.obj("hello" -> "harry".asJson))
      .uponReceiving("a request to say Goodbye")
      .path("/goodbye")
      .method("GET")
      .willRespondWith()
      .status(204)
      .given("bob exists")
      .uponReceiving("a request to find a friend")
      .path("/anyone-there")
      .method("GET")
      .willRespondWith()
      .status(200)
      .body(Json.obj("found" -> "bob".asJson))
      .toPact()

  override type Resources = Client[IO]

  override def additionalSharedResource: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].build

  test("weaver pact test") { resources =>
    val client = resources._1
    val server = resources._2
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(server.getUrl + "/hello"),
      headers = Headers(Header.Raw(CIString("other-header"), "howdy"))
    )
      .withEntity(Json.obj("name" -> "harry".asJson))
    client.run(request).use {
      _.as[String].map(body => expect(body == "{\"hello\":\"harry\"}"))
    }

  }

  test("another weaver pact test") { resources =>
    val client = resources._1
    val server = resources._2
    val request = Request[IO](
      uri = Uri.unsafeFromString(server.getUrl + "/goodbye"),
      headers = Headers(`Content-Type`(MediaType.application.json))
    )
    client.run(request).use {
      _.status.code.pure[IO].map(code => expect(code == 204))
    }
  }

  test("test with provider state") { resources =>
    val client  = resources._1
    val server  = resources._2
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/anyone-there"))
    client.run(request).use {
      _.as[String].map(body => expect(body == "{\"found\":\"bob\"}"))
    }
  }
}
