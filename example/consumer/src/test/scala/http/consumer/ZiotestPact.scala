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

package http.consumer

import au.com.dius.pact.consumer.{BaseMockServer, ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import io.circe.Json
import io.circe.syntax._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{BasicCredentials, Uri}
import pact4s.circe.implicits._
import pact4s.ziotest.RequestResponsePactForgerWith
import zio.interop.catz._
import zio.test._
import zio.{Scope, Task, ZIO, ZLayer}

import scala.annotation.nowarn

object ZiotestPact extends RequestResponsePactForgerWith[Client[Task]] with ExamplePactCommons {
  override val pactTestExecutionContext: PactTestExecutionContext = executionContext

  val pact: RequestResponsePact =
    ConsumerPactBuilder
      .consumer("zio-consumer")
      .hasPactWith("zio-provider")
      // -------------------------- FETCH RESOURCE --------------------------
      .`given`(
        "resource exists", // this is a state identifier that is passed to the provider
        Map[String, Any](
          "id"    -> testID,
          "value" -> 123
        ) // we can use parameters to specify details about the provider state
      )
      .uponReceiving("Request to fetch extant resource")
      .method("GET")
      .path(s"/resource/$testID")
      .headers("Authorization" -> mkAuthHeader("pass"))
      .willRespondWith()
      .status(200)
      .body(
        Json.obj("id" -> testID.asJson, "value" -> 123.asJson)
      ) // can use circe json directly for both request and response bodies with `import pact4s.circe.implicits._`
      .`given`("resource does not exist")
      .uponReceiving("Request to fetch missing resource")
      .method("GET")
      .path(s"/resource/$missingID")
      .headers("Authorization" -> mkAuthHeader("pass"))
      .willRespondWith()
      .status(404)
      .uponReceiving("Request to fetch resource with wrong auth")
      .method("GET")
      .path(s"/resource/$testID")
      .headers("Authorization" -> mkAuthHeader("wrong"))
      .willRespondWith()
      .status(401)
      // -------------------------- CREATE RESOURCE --------------------------
      .`given`("resource does not exist")
      .uponReceiving("Request to create new resource")
      .method("POST")
      .path("/resource")
      .headers("Authorization" -> mkAuthHeader("pass"))
      .body(newResource) // can use classes directly in the body if they are encodable
      .willRespondWith()
      .status(204)
      .`given`(
        "resource exists",
        Map[String, Any]("id" -> conflictResource.id, "value" -> conflictResource.value)
      ) // notice we're using the same state, but with different parameters
      .uponReceiving("Request to create resource that already exists")
      .method("POST")
      .path("/resource")
      .headers("Authorization" -> mkAuthHeader("pass"))
      .body(conflictResource)
      .willRespondWith()
      .status(409)
      .toPact

  override def resources: ZLayer[Scope, Nothing, Client[Task]] =
    EmberClientBuilder
      .default[Task]
      .build
      .toManagedZIO
      .toLayer
      .catchAll(e => sys.error(s"Failed to create client: $e")): @nowarn

  /* we should use these tests to ensure that our client class correctly handles responses from the provider - i.e. decoding, error mapping, validation
   */
  override def tests: Seq[Spec[Client[Task] with BaseMockServer, Nothing]] = List(
    test("handle fetch request for extant resource") {
      for {
        mockServer <- ZIO.service[BaseMockServer]
        client     <- ZIO.service[Client[Task]]
        res <- new ProviderClientImpl[Task](
          client,
          Uri.unsafeFromString(mockServer.getUrl),
          BasicCredentials("user", "pass")
        ).fetchResource(testID)
      } yield assertTrue(res == Some(ProviderResource(testID, 123)))
    },
    test("handle fetch request for missing resource") {
      for {
        mockServer <- ZIO.service[BaseMockServer]
        client     <- ZIO.service[Client[Task]]
        res <- new ProviderClientImpl[Task](
          client,
          Uri.unsafeFromString(mockServer.getUrl),
          BasicCredentials("user", "pass")
        ).fetchResource(missingID)
      } yield assertTrue(res == None)
    },
    test("handle fetch request with incorrect auth") {
      for {
        mockServer <- ZIO.service[BaseMockServer]
        client     <- ZIO.service[Client[Task]]
        res <- new ProviderClientImpl[Task](
          client,
          Uri.unsafeFromString(mockServer.getUrl),
          BasicCredentials("user", "wrong")
        ).fetchResource(testID).exit
      } yield assertTrue(
        res.causeOption.map(_.squashWith(identity)).get.isInstanceOf[InvalidCredentials.type]
      )
    },
    test("handle create request for new resource") {
      for {
        mockServer <- ZIO.service[BaseMockServer]
        client     <- ZIO.service[Client[Task]]
        _ <- new ProviderClientImpl[Task](
          client,
          Uri.unsafeFromString(mockServer.getUrl),
          BasicCredentials("user", "pass")
        ).createResource(newResource)
      } yield assertTrue(true)
    },
    test("handle create request for existing resource") {
      for {
        mockServer <- ZIO.service[BaseMockServer]
        client     <- ZIO.service[Client[Task]]
        res <- new ProviderClientImpl[Task](
          client,
          Uri.unsafeFromString(mockServer.getUrl),
          BasicCredentials("user", "pass")
        ).createResource(conflictResource).exit
      } yield assertTrue(res.causeOption.map(_.squashWith(identity)).get.isInstanceOf[UserAlreadyExists.type])
    }
  ).map(_.mapError(e => sys.error(s"Failed: ${e.getMessage}")))
}
