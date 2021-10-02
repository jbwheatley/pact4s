package pact4s.munit
import munit.CatsEffectSuite
import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}

class RequestResponsePactVerifierBrokerMUnitSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo("Pact4sProvider")

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    verifyPacts(
      publishVerificationResults = Some(
        PublishVerificationResults(
          providerVersion = "SNAPSHOT"
        )
      )
    )
  }
}
