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

package pact4s.munit.requestresponse

import munit.{AnyFixture, CatsEffectSuite}
import pact4s.MockProviderServer
import pact4s.munit.PactVerifier
import pact4s.provider.ProviderInfoBuilder

class PactVerifierStateChangeFunctionSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49172)

  override val provider: ProviderInfoBuilder = mock
    .fileSourceProviderInfo(
      fileName = "./scripts/Pact4sConsumer-Pact4sProvider-StateChange.json",
      useStateChangeFunction = true
    )

  override val munitFixtures: Seq[AnyFixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    verifyPacts()
  }
}
