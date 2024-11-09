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
import pact4s.provider.{ConsumerVersionSelectors, ProviderInfoBuilder}

class PactVerifierBrokerFeatureBranchSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49164, hasFeatureX = true)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(consumerVersionSelector = ConsumerVersionSelectors.branch("feat/x"))

  override val munitFixtures: Seq[AnyFixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider` with a feature branch, munit") {
    verifyPacts() *> mock.featureXState.tryGet.assertEquals(Some(true))
  }
}
