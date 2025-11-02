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

package pact4s.ziotest.message

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.messaging.{Message, MessagePact}
import io.circe.Json
import pact4s.TestModels
import pact4s.circe.implicits.messagePactDecoder
import pact4s.ziotest.MessagePactForger
import zio.ZIO
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object PactForgerSuite extends ZIOSpecDefault with MessagePactForger {
  override def pact: MessagePact                                  = TestModels.messagePact
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./ziotest-pact/target/pacts"
  )
  override val specName: String = "MessagePactForgerSuite"

  override def verify(message: Message): Spec[Any, Nothing] = message.getDescription match {
    case "A message to say hello" =>
      test("A message to say hello should responded to with a message containing `harry`")(
        for {
          _ <- ZIO.unit
          containedHello = message.as[Json].flatMap(_.hcursor.get[String]("hello"))
          metadataHi     = message.metadata.getOrElse("hi", "")
        } yield assertTrue(
          containedHello.isRight && containedHello.getOrElse("").equals("harry") && metadataHi.equals("there")
        )
      )

    case "A message to say goodbye" =>
      test("A message to say goodbye should be responded to with a message containing `harry`")(for {
        _ <- ZIO.unit
        containedGoodbye = message.as[Json].flatMap(_.hcursor.get[String]("goodbye"))
      } yield assertTrue(containedGoodbye.isRight && containedGoodbye.getOrElse("").equals("harry")))

    case "A message with nested arrays in the body" =>
      test("A message with nested Int arrays should be responded to with [1, 2, 3]")(for {
        _ <- ZIO.unit
        containedArray = message.as[Json].flatMap(_.hcursor.get[List[Int]]("array"))
      } yield assertTrue(containedArray.isRight && containedArray.getOrElse(List.empty).equals(List(1, 2, 3))))

    case "A message with a json array as content" =>
      test("A message with a json array should be responded to with (1, true)")(for {
        _ <- ZIO.unit
        res =
          for {
            json <- message.as[Json]
            acur = json.hcursor.downArray
            int  <- acur.get[Int]("a")
            bool <- acur.right.get[Boolean]("b")
          } yield (int, bool)
      } yield assertTrue(res.isRight && res.getOrElse((0, false)).equals((1, true))))

    case description => test(s"Missing verification for message: '$description'")(assertTrue(false))
  }
}
