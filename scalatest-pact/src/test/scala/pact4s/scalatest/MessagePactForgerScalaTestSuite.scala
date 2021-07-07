package pact4s.scalatest

import au.com.dius.pact.consumer.PactTestExecutionContext
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

  val pact: MessagePact = Pact4sMessagePactBuilder()
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
    .toMessagePact

  it should "scalatest message pact test" in {
    messages.head.as[Json].flatMap(_.hcursor.get[String]("hello")).getOrElse(fail()) shouldBe "harry"
    messages.head.metadata.getOrElse("hi", fail()) shouldBe "there"
  }

  it should "another scalatest message pact test" in {
    messages(1).as[Json].flatMap(_.hcursor.get[String]("goodbye")).getOrElse(fail()) shouldBe "harry"
  }

  it should "send a message with a nested array in its body" in {
    messages(2).as[Json].flatMap(_.hcursor.get[List[Int]]("array")).getOrElse(fail()) shouldBe List(1, 2, 3)
  }

  it should "send a message with a json array as its content" in {
    val res = for {
      json <- messages(3).as[Json]
      acur = json.hcursor.downArray
      int  <- acur.get[Int]("a")
      bool <- acur.right.get[Boolean]("b")
    } yield (int, bool)
    res.getOrElse(fail()) shouldBe ((1, true))
  }
}
