package pact4s.munit

import munit.CatsEffectSuite
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
import pact4s.{MockProviderServer, ProviderInfoBuilder}

class MessagePactVerifierMUnitSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(2347)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sMessageConsumer",
    providerName = "Pact4sMessageProvider",
    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json",
    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.messages")))
  )

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    verifyPacts()
  }
}
