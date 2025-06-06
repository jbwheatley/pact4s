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

package http.provider

import cats.effect.unsafe.implicits.global
import cats.implicits.{catsSyntaxTuple2Semigroupal, toFoldableOps, toSemigroupKOps}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.server.Server
import org.http4s.{BasicCredentials, HttpRoutes}
import pact4s.provider.ProviderRequestFilter.{NoOpFilter, SetHeaders}
import pact4s.provider.StateManagement.StateManagementFunction
import pact4s.provider._
import pact4s.ziotest.PactVerifier
import zio.interop.catz._
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, Task, Unsafe, ZLayer}

import java.io.File
import scala.annotation.nowarn
import scala.concurrent.duration.DurationInt

object ZioVerifyPacts extends ZIOSpecDefault with PactVerifier {

  def resource: ZLayer[Scope, Nothing, Server] = EmberServerBuilder
    .default[Task]
    .withHost(Host.fromString("localhost").get)
    .withPort(Port.fromInt(1235).get)
    .withHttpApp((fetchRoute <+> createRoute).orNotFound)
    .withShutdownTimeout(0.seconds)
    .build
    .toManagedZIO
    .toLayer
    .catchAll(e => sys.error(s"Failed to create client: $e")): @nowarn

  // Insert deliberately data that the provider state before hook should clean so that tests succeed
  val store: MockResourceStore[Task] = MockResourceStore.unsafe[Task](Map("missingId" -> 99, "newId" -> 66))

  val apiKey: String = "1dcbkjabyge1g273ie1u2"

  val fetchRoute: HttpRoutes[Task] = FetchRoute[Task](store.fetch, apiKey)

  val createRoute: HttpRoutes[Task] = CreateRoute[Task](store.create, apiKey)

  // If the auth header in the request is "correct", we can replace it with an auth header that will actually work with our API,
  // else we leave it as is to be rejected.
  def requestFilter: ProviderRequest => ProviderRequestFilter = req =>
    req.getFirstHeader("Authorization") match {
      case Some((_, value)) =>
        Authorization
          .parse(value)
          .map {
            case Authorization(BasicCredentials("user", "pass")) =>
              SetHeaders("Authorization" -> s"Basic ${BasicCredentials("user", apiKey).token}")
            case _ => NoOpFilter
          }
          .getOrElse(NoOpFilter)
      case None => NoOpFilter
    }

  val provider: ProviderInfoBuilder = ProviderInfoBuilder(
    "zio-provider",
    PactSource.FileSource(
      Map("zio-consumer" -> new File("./example/resources/pacts/zio-consumer-zio-provider.json"))
    )
  ).withHost("localhost")
    .withPort(1235)
    .withStateManagementFunction(
      StateManagementFunction
        .withResponse {
          case ProviderState("resource exists", params) =>
            val id    = params.get("id")
            val value = params.get("value").map(_.toInt)
            Unsafe.unsafe { implicit unsafe: Unsafe =>
              runtime.unsafe
                .run((id, value).mapN(ProviderResource.apply).traverse_(store.create).as(Map.empty[String, String]))
                .getOrThrow()
            }

          case ProviderState("resource with auto-generated ID exists", _) =>
            val id = "autoGeneratedId"
            Unsafe.unsafe { implicit unsafe: Unsafe =>
              runtime.unsafe
                .run(
                  store
                    .create(ProviderResource(id, 1001))
                    .as(Map("resourceId" -> id))
                )
                .getOrThrow()
            }

          case ProviderState("resource does not exist", _) => Map.empty // Nothing to do
          case _: ProviderState                            => Map.empty
        }
        .withBeforeEach(() => Unsafe.unsafe { implicit unsafe: Unsafe => runtime.unsafe.run(store.empty).getOrThrow() })
    )
    .withRequestFiltering(requestFilter)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    test("Verify pacts") {
      verifyPacts(
        publishVerificationResults = None,
        providerVerificationOptions = Nil,
        verificationTimeout = Some(10.seconds)
      ).as(assertTrue(true))
    }.provideLayerShared(resource)
}
