package pact4s.scalatest

import au.com.dius.pact.consumer.{MessagePactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.messaging.MessagePact
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.circe.implicits._

class MessagePactForgerScalaTestSuite extends AnyFlatSpec with Matchers with MessagePactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./scalatest-pact/target/pacts"
  )

  val pact: MessagePact = new MessagePactBuilder()
    .consumer("Pact4sMessageConsumer")
    .hasPactWith("Pact4sMessageProvider")
    .expectsToReceive("A message to say hello")
    .withContent(Json.obj("hello" -> "harry".asJson))
    .withMetadata(Map("hi" -> "there"))
    .expectsToReceive("A message to say goodbye")
    .withContent(Json.obj("goodbye" -> "harry".asJson))
    .toPact[MessagePact]

  it should "scalatest message pact test" in {
    messages.head.as[Json].flatMap(_.hcursor.get[String]("hello")).getOrElse(fail()) shouldBe "harry"
    messages.head.metadata.getOrElse("hi", fail()) shouldBe "there"
  }

  it should "another scalatest message pact test" in {
    messages(1).as[Json].flatMap(_.hcursor.get[String]("goodbye")).getOrElse(fail()) shouldBe "harry"
  }
}
