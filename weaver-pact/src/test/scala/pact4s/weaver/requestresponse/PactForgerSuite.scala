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

package pact4s.weaver.requestresponse

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.{IO, Resource}
import cats.syntax.all._
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.{CIString, CIStringSyntax}
import pact4s.TestModels
import pact4s.weaver.RequestResponsePactForger
import weaver.IOSuite

object PactForgerSuite extends IOSuite with RequestResponsePactForger[IO] {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./weaver-pact/target/pacts"
  )

  val pact: RequestResponsePact = TestModels.requestResponsePact

  override type Resources = Client[IO]

  override def additionalSharedResource: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].build

  test("weaver pact test") { resources =>
    val client  = resources._1
    val server  = resources._2
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(server.getUrl + "/hello"),
      headers = Headers(Header.Raw(CIString("other-header"), "howdy"))
    )
      .withEntity(Json.obj("name" -> "harry".asJson))
    client.run(request).use {
      _.as[String].map(body => expect(body == "{\"hello\":\"harry\"}"))
    }

  }

  test("another weaver pact test") { resources =>
    val client  = resources._1
    val server  = resources._2
    val request = Request[IO](
      uri = Uri.unsafeFromString(server.getUrl + "/goodbye"),
      headers = Headers(`Content-Type`(MediaType.application.json))
    )
    client.run(request).use {
      _.status.code.pure[IO].map(code => expect(code == 204))
    }
  }

  test("test with provider state") { resources =>
    val client  = resources._1
    val server  = resources._2
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/anyone-there/bob"))
    client.run(request).use {
      _.as[String].map(body => expect(body == "{\"found\":\"bob\"}"))
    }
  }

  test("test with provider state no params") { resources =>
    val client  = resources._1
    val server  = resources._2
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/anyone-there"))
    client.run(request).use {
      _.status.code.pure[IO].map(code => expect(code == 404))
    }
  }

  test("test with generated auth header") { resources =>
    val client  = resources._1
    val server  = resources._2
    val request = Request[IO](uri = Uri.unsafeFromString(server.getUrl + "/authorized"))
      .putHeaders(headers.Authorization(Credentials.Token(ci"Bearer", "super-secure")))
    client
      .run(request)
      .use {
        _.status.code.pure[IO].map(code => expect(code == 200))
      }
  }
}
