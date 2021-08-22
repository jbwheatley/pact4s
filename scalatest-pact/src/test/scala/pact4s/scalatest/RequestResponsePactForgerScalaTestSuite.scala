package pact4s.scalatest

import au.com.dius.pact.consumer.{ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.{RequestResponseInteraction, RequestResponsePact}
import cats.effect.IO
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.{Header, Headers, MediaType, Method, Request, Uri}
import org.typelevel.ci.CIString
import pact4s.circe.implicits._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.headers.`Content-Type`
import org.scalatest.Assertion

class RequestResponsePactForgerScalaTestSuite extends AnyFlatSpec with Matchers with RequestResponsePactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./scalatest-pact/target/pacts"
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

  val client: Client[IO] = EmberClientBuilder.default[IO].build.allocated.unsafeRunSync()._1

  def verify(interaction: RequestResponseInteraction): Assertion = interaction.getDescription match {
    case "a request to say Hello" =>
      val request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(mockServer.getUrl + "/hello"),
        headers = Headers(Header.Raw(CIString("other-header"), "howdy"))
      )
        .withEntity(Json.obj("name" -> "harry".asJson))
      client
        .run(request)
        .use {
          _.as[String].map(_ shouldBe "{\"hello\":\"harry\"}")
        }
        .unsafeRunSync()
    case "a request to say Goodbye" =>
      val request = Request[IO](
        uri = Uri.unsafeFromString(mockServer.getUrl + "/goodbye"),
        headers = Headers(`Content-Type`(MediaType.application.json))
      )
      client
        .run(request)
        .use {
          _.status.code.pure[IO].map(_ shouldBe 204)
        }
        .unsafeRunSync()
    case "a request to find a friend" =>
      val request = Request[IO](uri = Uri.unsafeFromString(mockServer.getUrl + "/anyone-there"))
      client
        .run(request)
        .use {
          _.as[String].map(_ shouldBe "{\"found\":\"bob\"}")
        }
        .unsafeRunSync()
    case description => fail(s"Missing verification for message: '$description'.'")
  }

  interactions.foreach { interaction =>
    it should s"forge: ${interaction.getDescription}" in verify(interaction)
  }
}
