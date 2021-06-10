package pact4s.munit

import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings

class MessagePactVerifierBrokerMUnitSuite extends PactVerifier {
  val mock = new MockProviderServer(2348)

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(
    providerName = "Pact4sMessageProvider",
    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.messages")))
  )

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  verifyPacts(
    publishVerificationResults = Some(
      PublishVerificationResults(
        providerVersion = "SNAPSHOT",
        providerTags = Nil
      )
    )
  )
}
