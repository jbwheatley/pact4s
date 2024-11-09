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

package pact4s.scalatest.requestresponse

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.{RequestResponseInteraction, RequestResponsePact}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.`Content-Type`
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.{CIString, CIStringSyntax}
import pact4s.TestModels
import pact4s.scalatest.RequestResponsePactForger

class PactForgerSuite extends AnyFlatSpec with Matchers with RequestResponsePactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./scalatest-pact/target/pacts"
  )

  val pact: RequestResponsePact = TestModels.requestResponsePact

  val client: Client[IO] = EmberClientBuilder.default[IO].build.allocated.unsafeRunSync()._1

  def verify(interaction: RequestResponseInteraction): Assertion = interaction.getDescription match {
    case "a request to say Hello" =>
      val request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(mockServer.getUrl + "/hello"),
        headers = Headers(Header.Raw(CIString("other-header"), "howdy"))
      )
        .withEntity(Json.obj("name" -> "harry".asJson))
      client
        .run(request)
        .use {
          _.as[String].map(_ shouldBe "{\"hello\":\"harry\"}")
        }
        .unsafeRunSync()
    case "a request to say Goodbye" =>
      val request = Request[IO](
        uri = Uri.unsafeFromString(mockServer.getUrl + "/goodbye"),
        headers = Headers(`Content-Type`(MediaType.application.json))
      )
      client
        .run(request)
        .use {
          _.status.code.pure[IO].map(_ shouldBe 204)
        }
        .unsafeRunSync()
    case "a request to find a friend" =>
      val request = Request[IO](uri = Uri.unsafeFromString(mockServer.getUrl + "/anyone-there/bob"))
      client
        .run(request)
        .use {
          _.as[String].map(_ shouldBe "{\"found\":\"bob\"}")
        }
        .unsafeRunSync()
    case "a request to find anyone" =>
      val request = Request[IO](uri = Uri.unsafeFromString(mockServer.getUrl + "/anyone-there"))
      client
        .run(request)
        .use {
          _.status.code.pure[IO].map(_ shouldBe 404)
        }
        .unsafeRunSync()
    case "a request with auth header" =>
      val request = Request[IO](uri = Uri.unsafeFromString(mockServer.getUrl + "/authorized"))
        .putHeaders(headers.Authorization(Credentials.Token(ci"Bearer", "super-secure")))
      client
        .run(request)
        .use {
          _.status.code.pure[IO].map(_ shouldBe 200)
        }
        .unsafeRunSync()
    case description => fail(s"Missing verification for message: '$description'.'")
  }

  interactions.foreach { interaction =>
    it should s"forge: ${interaction.getDescription}" in verify(interaction)
  }
}
