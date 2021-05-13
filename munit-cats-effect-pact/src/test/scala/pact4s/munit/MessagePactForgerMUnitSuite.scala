package pact4s.munit
import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.core.model.messaging.MessagePact
import cats.effect.IO
import io.circe.Json
import io.circe.syntax.EncoderOps
import pact4s.circe.implicits._

class MessagePactForgerMUnitSuite extends MessagePactForger {
  val pact: MessagePact = MessagePactBuilder
    .consumer("MessageConsumer")
    .hasPactWith("MessageProvider")
    .expectsToReceive("A message to say hello")
    .withContent(Json.obj("hello" -> "harry".asJson))
    .withMetadata(Map("hi" -> "there"))
    .expectsToReceive("A message to say goodbye")
    .withContent(Json.obj("goodbye" -> "harry".asJson))
    .toPact

  test("munit message pact test") {
    IO.fromEither(messages.head.as[Json].flatMap(_.hcursor.get[String]("hello"))).assertEquals("harry") *>
      IO.fromOption(messages.head.metadata.get("hi"))(new Exception()).assertEquals("there")
  }

  test("another munit message pact test") {
    IO.fromEither(messages(1).as[Json].flatMap(_.hcursor.get[String]("goodbye"))).assertEquals("harry")
  }
}
