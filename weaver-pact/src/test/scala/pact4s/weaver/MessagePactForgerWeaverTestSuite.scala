package pact4s.weaver

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.messaging.MessagePact
import cats.effect.IO
import io.circe.Json
import io.circe.syntax.EncoderOps
import pact4s.circe.implicits._
import weaver.IOSuite

object MessagePactForgerWeaverTestSuite extends IOSuite with SimpleMessagePactForger[IO] {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./weaver-pact/target/pacts"
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

  test("weaver message pact test") { messages =>
    IO.fromEither(messages.head.as[Json].flatMap(_.hcursor.get[String]("hello")))
      .map(s => expect(s == "harry")) *>
      IO.fromOption(messages.head.metadata.get("hi"))(new Exception()).map(s => expect(s == "there"))
  }

  test("another weaver message pact test") { messages =>
    IO.fromEither(messages(1).as[Json].flatMap(_.hcursor.get[String]("goodbye"))).map(s => expect(s == "harry"))
  }
}
