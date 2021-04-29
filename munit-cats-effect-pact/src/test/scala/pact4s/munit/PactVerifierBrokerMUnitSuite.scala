package pact4s.munit
import pact4s.{MockProviderServer, ProviderInfoBuilder}

class PactVerifierBrokerMUnitSuite extends PactVerifier {
  val mock = new MockProviderServer(2346)

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  verifyPacts()
}
