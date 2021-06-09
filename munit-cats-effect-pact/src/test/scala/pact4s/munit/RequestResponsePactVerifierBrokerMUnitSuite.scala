package pact4s.munit
import pact4s.{MockProviderServer, ProviderInfoBuilder, VerificationType}

class RequestResponsePactVerifierBrokerMUnitSuite extends PactVerifier {
  val mock = new MockProviderServer(2346)

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(
    providerName = "Pact4sProvider",
    verificationType = VerificationType.RequestResponse
  )

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  verifyPacts()
}
