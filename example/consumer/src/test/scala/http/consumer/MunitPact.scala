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

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.{ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.IO
import io.circe.Json
import io.circe.syntax._
import munit.AnyFixture
import munit.catseffect.IOFixture
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{BasicCredentials, Uri}
import pact4s.circe.implicits._
import pact4s.munit.RequestResponsePactForger

class MunitPact extends RequestResponsePactForger with ExamplePactCommons {
  override val pactTestExecutionContext: PactTestExecutionContext = executionContext

  override val mockProviderConfig: MockProviderConfig = MockProviderConfig.httpConfig("localhost", 1234)

  val pact: RequestResponsePact =
    ConsumerPactBuilder
      .consumer("munit-consumer")
      .hasPactWith("munit-provider")
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

  val clientFixture: IOFixture[Client[IO]] = ResourceSuiteLocalFixture(
    "httpClient",
    EmberClientBuilder.default[IO].build
  )

  /*
  As pact4s uses an munit fixture to manage the mock provider that it uses to run these tests against,
  any additional fixtures must be specified by overriding this method, rather than `munitFixtures`
   */
  override def additionalMunitFixtures: Seq[AnyFixture[_]] = Seq(clientFixture)
  /*
  we should use these tests to ensure that our client class correctly handles responses from the provider - i.e. decoding, error mapping, validation
   */
  pactTest("handle fetch request for extant resource") { mockServer =>
    new ProviderClientImpl[IO](
      clientFixture(),
      Uri.unsafeFromString(mockServer.getUrl),
      BasicCredentials("user", "pass")
    )
      .fetchResource(testID)
      .assertEquals(Some(ProviderResource(testID, 123)))
  }

  pactTest("handle fetch request for missing resource") { mockServer =>
    new ProviderClientImpl[IO](
      clientFixture(),
      Uri.unsafeFromString(mockServer.getUrl),
      BasicCredentials("user", "pass")
    )
      .fetchResource(missingID)
      .assertEquals(None)
  }

  pactTest("handle fetch request with incorrect auth") { mockServer =>
    new ProviderClientImpl[IO](
      clientFixture(),
      Uri.unsafeFromString(mockServer.getUrl),
      BasicCredentials("user", "wrong")
    )
      .fetchResource(testID)
      .intercept[InvalidCredentials.type]
  }

  pactTest("handle create request for new resource") { mockServer =>
    new ProviderClientImpl[IO](
      clientFixture(),
      Uri.unsafeFromString(mockServer.getUrl),
      BasicCredentials("user", "pass")
    )
      .createResource(newResource)
      .assert
  }

  pactTest("handle create request for existing resource") { mockServer =>
    new ProviderClientImpl[IO](
      clientFixture(),
      Uri.unsafeFromString(mockServer.getUrl),
      BasicCredentials("user", "pass")
    )
      .createResource(conflictResource)
      .intercept[UserAlreadyExists.type]
  }
}
