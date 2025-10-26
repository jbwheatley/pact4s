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

package pact4s.ziotest.requestresponse

import au.com.dius.pact.consumer.{BaseMockServer, PactTestExecutionContext}
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
import org.typelevel.ci.{CIString, CIStringSyntax}
import pact4s.TestModels
import pact4s.ziotest.RequestResponsePactForger
import zio.ZIO
import zio.test.{Spec, assertTrue}

object PactForgerSuite extends RequestResponsePactForger {

  val client: Client[IO] = EmberClientBuilder.default[IO].build.allocated.unsafeRunSync()._1

  override def pact: RequestResponsePact                          = TestModels.requestResponsePact
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./ziotest-pact/target/pacts"
  )
  override def specName: String = "RequestResponsePactForgerSuite"
  override def verify(interaction: RequestResponseInteraction): Spec[BaseMockServer, Nothing] =
    interaction.getDescription match {
      case "a request to say Hello" =>
        test("A request to say Hello should return a response with harry")(for {
          mockServer <- ZIO.service[BaseMockServer]
          request = Request[IO](
            method = Method.POST,
            uri = Uri.unsafeFromString(mockServer.getUrl + "/hello"),
            headers = Headers(Header.Raw(CIString("other-header"), "howdy"))
          )
            .withEntity(Json.obj("name" -> "harry".asJson))
          response = client.run(request).use(_.as[String]).unsafeRunSync()
        } yield assertTrue(response.equals("{\"hello\":\"harry\"}")))

      case "a request to say Goodbye" =>
        test("A request to say goodbye should return HTTP status 204")(for {
          mockServer <- ZIO.service[BaseMockServer]
          request = Request[IO](
            uri = Uri.unsafeFromString(mockServer.getUrl + "/goodbye"),
            headers = Headers(`Content-Type`(MediaType.application.json))
          )
          response = client.run(request).use(_.status.code.pure[IO]).unsafeRunSync()
        } yield assertTrue(response == 204))

      case "a request to find a friend" =>
        test("A request to find a friend named bob should return found")(for {
          mockServer <- ZIO.service[BaseMockServer]
          request = Request[IO](
            uri = Uri.unsafeFromString(mockServer.getUrl + "/anyone-there/bob")
          )
          response = client.run(request).use(_.as[String]).unsafeRunSync()
        } yield assertTrue(response.equals("{\"found\":\"bob\"}")))

      case "a request to find anyone" =>
        test("A request to find anyone should return HTTP status code 404")(for {
          mockServer <- ZIO.service[BaseMockServer]
          request = Request[IO](
            uri = Uri.unsafeFromString(mockServer.getUrl + "/anyone-there")
          )
          response = client.run(request).use(_.status.code.pure[IO]).unsafeRunSync()
        } yield assertTrue(response == 404))

      case "a request with auth header" =>
        test("A request to /authorized with auth header should return HTTP status code 200")(for {
          mockServer <- ZIO.service[BaseMockServer]
          request = Request[IO](
            uri = Uri.unsafeFromString(mockServer.getUrl + "/authorized")
          )
            .putHeaders(headers.Authorization(Credentials.Token(ci"Bearer", "super-secure")))
          response = client.run(request).use(_.status.code.pure[IO]).unsafeRunSync()
        } yield assertTrue(response == 200))

      case _ => test(s"Missing verification for interaction '${interaction.getDescription}'")(assertTrue(false))
    }

}
