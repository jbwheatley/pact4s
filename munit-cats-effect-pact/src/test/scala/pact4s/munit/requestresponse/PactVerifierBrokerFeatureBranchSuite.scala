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
