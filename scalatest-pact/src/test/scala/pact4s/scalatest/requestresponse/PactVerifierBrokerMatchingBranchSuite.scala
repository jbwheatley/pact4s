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

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.MockProviderServer
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder}
import pact4s.scalatest.PactVerifier

import scala.concurrent.duration._

class PactVerifierBrokerMatchingBranchSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll with Matchers {
  val mock = new MockProviderServer(49300, hasFeatureX = true)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(
      consumerVersionSelector = ConsumerVersionSelectors.matchingBranch,
      pendingPactsEnabled = true
    )

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit =
    cleanUp.unsafeRunSync()

  it should "Verify pacts for provider `Pact4sProvider` with a feature branch and matching branch selector, scalatest" in {
    verifyPacts(
      Some(Branch("feat/x"))
    )
    mock.featureXState.get.unsafeRunTimed(10.seconds) shouldBe Some(true)
  }
}
