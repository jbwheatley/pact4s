package pact4s.scalatest.message

import au.com.dius.pact.consumer.{MessagePactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.messaging.{Message, MessagePact}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.circe.implicits._
import pact4s.scalatest.MessagePactForger

class PactForgerSuite extends AnyFlatSpec with Matchers with MessagePactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./scalatest-pact/target/pacts"
  )

  val pact: MessagePact = MessagePactBuilder
    .consumer("Pact4sMessageConsumer")
    .hasPactWith("Pact4sMessageProvider")
    .expectsToReceive("A message to say hello")
    .withContent(Json.obj("hello" -> "harry".asJson))
    .withMetadata(Map("hi" -> "there"))
    .expectsToReceive("A message to say goodbye")
    .withContent(Json.obj("goodbye" -> "harry".asJson))
    .expectsToReceive("A message with nested arrays in the body")
    .withContent(Json.obj("array" -> List(1, 2, 3).asJson))
    .expectsToReceive("A message with a json array as content")
    .withContent(Json.arr(Json.obj("a" -> 1.asJson), Json.obj("b" -> true.asJson)))
    .toPact

  def verify(message: Message): Assertion = message.getDescription match {
    case "A message to say hello" =>
      message.as[Json].flatMap(_.hcursor.get[String]("hello")).getOrElse(fail()) shouldBe "harry"
      message.metadata.getOrElse("hi", fail()) shouldBe "there"
    case "A message to say goodbye" =>
      message.as[Json].flatMap(_.hcursor.get[String]("goodbye")).getOrElse(fail()) shouldBe "harry"
    case "A message with nested arrays in the body" =>
      message.as[Json].flatMap(_.hcursor.get[List[Int]]("array")).getOrElse(fail()) shouldBe List(1, 2, 3)
    case "A message with a json array as content" =>
      val res = for {
        json <- message.as[Json]
        acur = json.hcursor.downArray
        int  <- acur.get[Int]("a")
        bool <- acur.right.get[Boolean]("b")
      } yield (int, bool)
      res.getOrElse(fail()) shouldBe ((1, true))
    case description => fail(s"Missing verification for message: '$description'.'")
  }

  messages.foreach { message =>
    it should s"forge: ${message.getDescription}" in verify(message)
  }
}
