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
    .expectsToReceive("A message with nested arrays in the body")
    .withContent(Json.obj("array" -> List(1, 2, 3).asJson))
    .expectsToReceive("A message with a json array as content")
    .withContent(Json.arr(Json.obj("a" -> 1.asJson), Json.obj("b" -> true.asJson)))
    .toMessagePact

  test("weaver message pact test") { messages =>
    IO.fromEither(messages.head.as[Json].flatMap(_.hcursor.get[String]("hello")))
      .map(s => expect(s == "harry")) *>
      IO.fromOption(messages.head.metadata.get("hi"))(new Exception()).map(s => expect(s == "there"))
  }

  test("another weaver message pact test") { messages =>
    IO.fromEither(messages(1).as[Json].flatMap(_.hcursor.get[String]("goodbye"))).map(s => expect(s == "harry"))
  }

  test("send a message with a nested array in its body") { messages =>
    IO.fromEither(messages(2).as[Json].flatMap(_.hcursor.get[List[Int]]("array")))
      .map(list => expect(list == List(1, 2, 3)))
  }

  test("send a message with a json array as its content") { messages =>
    val res = for {
      json <- messages(3).as[Json]
      acur = json.hcursor.downArray
      int  <- acur.get[Int]("a")
      bool <- acur.right.get[Boolean]("b")
    } yield (int, bool)
    IO.fromEither(res).map(tup => expect(tup == ((1, true))))
  }
}
