package pact4s.munit.requestresponse

import cats.effect.IO
import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.munit.PactVerifier
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder}

class PactVerifierBrokerMatchingBranchSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49264, hasFeatureX = true)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(
      consumerVersionSelector = ConsumerVersionSelectors.matchingBranch,
      pendingPactsEnabled = true
    )

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider` with a feature branch and matching branch selector, munit") {
    IO(
      verifyPacts(
        Some(
          Branch("feat/x")
        )
      )
    ) *> mock.featureXState.tryGet.assertEquals(Some(true))
  }
}
