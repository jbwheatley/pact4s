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

package http
package consumer

import au.com.dius.pact.consumer.PactTestExecutionContext
import cats.effect.IO
import cats.effect.unsafe.implicits._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

import java.util.Base64

trait ExamplePactCommons {
  /*
  we can define the folder that the pact contracts get written to upon completion of this test suite.
   */
  val executionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./example/resources/pacts"
  )

  protected val testID           = "testID"
  protected val missingID        = "missingID"
  protected val newResource      = ProviderResource("newID", 234)
  protected val conflictResource = ProviderResource("conflict", 234)

  protected def mkAuthHeader(pass: String) = s"Basic ${Base64.getEncoder.encodeToString(s"user:$pass".getBytes)}"

  lazy val client: Client[IO] = EmberClientBuilder.default[IO].build.allocated.unsafeRunSync()._1
}
