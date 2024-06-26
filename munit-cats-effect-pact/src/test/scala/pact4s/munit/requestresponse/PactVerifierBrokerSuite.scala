package pact4s.munit.requestresponse

import munit.{AnyFixture, CatsEffectSuite}
import pact4s.MockProviderServer
import pact4s.munit.PactVerifier
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder}

class PactVerifierBrokerSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49154)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(consumerVersionSelector = ConsumerVersionSelectors.mainBranch)

  override val munitFixtures: Seq[AnyFixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`, munit") {
    verifyPacts(
      Some(
        Branch.MAIN
      )
    ) *> mock.featureXState.tryGet.assertEquals(None)
  }
}
