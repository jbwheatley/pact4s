package pact4s.weaver

import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
import weaver.SimpleIOSuite

object MessagePactVerifierBrokerWeaverTestSuite extends SimpleIOSuite with PactVerifier {
  val mock = new MockProviderServer(1237)

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(
    providerName = "Pact4sMessageProvider",
    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.messages")))
  )

  pureTest("Verify pacts for provider `MessageProvider`") {
    succeed(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT",
            providerTags = Nil
          )
        )
      )
    )
  }

}
