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

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.MockProviderServer
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder}
import pact4s.weaver.PactVerifier
import weaver.IOSuite

object PactVerifierBrokerFeatureBranchSuite extends IOSuite with PactVerifier[IO] {
  type Res = Server

  val mock = new MockProviderServer(49173, hasFeatureX = true)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(consumerVersionSelector = ConsumerVersionSelectors.branch("feat/x"))

  test("Verify pacts for provider `Pact4sProvider` with a feature branch, weaver") {
    for {
      a <-
        verifyPacts(
          Some(
            Branch("feat/x")
          )
        ).map(succeed)
      x <- mock.featureXState.tryGet
    } yield a && expect(x.contains(true))
  }
}
