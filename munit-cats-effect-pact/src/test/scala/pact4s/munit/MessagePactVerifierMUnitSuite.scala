package pact4s.munit

import pact4s.{MockProviderServer, ProviderInfoBuilder}
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings

class MessagePactVerifierMUnitSuite extends PactVerifier {
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

  verifyPacts()
}
