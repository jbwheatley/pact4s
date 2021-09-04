package pact4s.weaver

import pact4s.{MockProviderServer, ProviderInfoBuilder}
import pact4s.messages.MessagesProvider
import weaver.SimpleIOSuite

object MessagePactVerifierWeaverTestSuite extends SimpleIOSuite with MessagePactVerifier {
  val mock = new MockProviderServer(1237)

  def messages: ResponseFactory = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sMessageConsumer",
    providerName = "Pact4sMessageProvider",
    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json"
  )

  pureTest("Verify pacts for provider `MessageProvider`") {
    succeed(verifyPacts())
  }
}
