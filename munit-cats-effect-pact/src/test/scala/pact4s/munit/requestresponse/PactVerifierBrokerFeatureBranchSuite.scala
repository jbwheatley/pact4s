package pact4s.munit.requestresponse

import cats.effect.IO
import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.munit.PactVerifier
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder, PublishVerificationResults}

class PactVerifierBrokerFeatureBranchSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49164, hasFeatureX = true)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider", consumerVersionSelector = ConsumerVersionSelectors.branch("feat/x"))

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider` with a feature branch, munit") {
    IO(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT",
            providerBranch = Branch("feat/x")
          )
        )
      )
    ) *> mock.featureXState.tryGet.assertEquals(Some(true))
  }
}
