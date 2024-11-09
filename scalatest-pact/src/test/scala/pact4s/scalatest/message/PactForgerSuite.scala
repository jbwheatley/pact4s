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

package pact4s.scalatest.message

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.messaging.{Message, MessagePact}
import io.circe.Json
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.TestModels
import pact4s.circe.implicits._
import pact4s.scalatest.MessagePactForger

class PactForgerSuite extends AnyFlatSpec with Matchers with MessagePactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./scalatest-pact/target/pacts"
  )

  val pact: MessagePact = TestModels.messagePact

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
