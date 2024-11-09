/*
 * Copyright 2021 io.github.jbwheatley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pact4s.weaver.message

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.messaging.MessagePact
import cats.effect.IO
import io.circe.Json
import pact4s.TestModels
import pact4s.circe.implicits._
import pact4s.weaver.SimpleMessagePactForger
import weaver.IOSuite

object PactForgerSuite extends IOSuite with SimpleMessagePactForger[IO] {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./weaver-pact/target/pacts"
  )

  val pact: MessagePact = TestModels.messagePact

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
