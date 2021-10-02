package pact4s.weaver

import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.messages.MessagesProvider
import weaver.SimpleIOSuite

object MessagePactVerifierBrokerWeaverTestSuite extends SimpleIOSuite with MessagePactVerifier {
  val mock = new MockProviderServer(isRequestResponse = false)

  def messages: ResponseFactory              = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(providerName = "Pact4sMessageProvider")

  pureTest("Verify pacts for provider `MessageProvider`") {
    succeed(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT"
          )
        )
      )
    )
  }
}
