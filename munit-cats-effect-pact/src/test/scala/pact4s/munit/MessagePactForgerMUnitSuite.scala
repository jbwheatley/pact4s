package pact4s.munit
import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.messaging.MessagePact
import cats.effect.IO
import io.circe.Json
import io.circe.syntax.EncoderOps
import pact4s.circe.implicits._

class MessagePactForgerMUnitSuite extends MessagePactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./munit-cats-effect-pact/target/pacts"
  )

  val pact: MessagePact = Pact4sMessagePactBuilder()
    .consumer("Pact4sMessageConsumer")
    .hasPactWith("Pact4sMessageProvider")
    .expectsToReceive("A message to say hello")
    .withContent(Json.obj("hello" -> "harry".asJson))
    .withMetadata(Map("hi" -> "there"))
    .expectsToReceive("A message to say goodbye")
    .withContent(Json.obj("goodbye" -> "harry".asJson))
    .toMessagePact

  test("munit message pact test") {
    IO.fromEither(messages.head.as[Json].flatMap(_.hcursor.get[String]("hello"))).assertEquals("harry") *>
      IO.fromOption(messages.head.metadata.get("hi"))(new Exception()).assertEquals("there")
  }

  test("another munit message pact test") {
    IO.fromEither(messages(1).as[Json].flatMap(_.hcursor.get[String]("goodbye"))).assertEquals("harry")
  }
}
