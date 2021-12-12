package pact4s.munit
import munit.CatsEffectSuite
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}
import pact4s.MockProviderServer

class RequestResponsePactVerifierBrokerMUnitSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49154)

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
