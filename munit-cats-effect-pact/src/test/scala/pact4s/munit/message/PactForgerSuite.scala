package pact4s.munit.message

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.messaging.MessagePact
import cats.effect.IO
import io.circe.Json
import pact4s.TestModels
import pact4s.circe.implicits._
import pact4s.munit.MessagePactForger

class PactForgerSuite extends MessagePactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./munit-cats-effect-pact/target/pacts"
  )

  val pact: MessagePact = TestModels.messagePact

  test("munit message pact test") {
    IO.fromEither(messages.head.as[Json].flatMap(_.hcursor.get[String]("hello"))).assertEquals("harry") *>
      IO.fromOption(messages.head.metadata.get("hi"))(new Exception()).assertEquals("there")
  }

  test("another munit message pact test") {
    IO.fromEither(messages(1).as[Json].flatMap(_.hcursor.get[String]("goodbye"))).assertEquals("harry")
  }

  test("send a message with a nested array in its body") {
    IO.fromEither(messages(2).as[Json].flatMap(_.hcursor.get[List[Int]]("array"))).assertEquals(List(1, 2, 3))
  }

  test("send a message with a json array as its content") {
    val res = for {
      json <- messages(3).as[Json]
      acur = json.hcursor.downArray
      int  <- acur.get[Int]("a")
      bool <- acur.right.get[Boolean]("b")
    } yield (int, bool)
    IO.fromEither(res).assertEquals((1, true))
  }
}
