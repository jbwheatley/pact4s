package pact4s.weaver

import cats.effect.IO
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.ProviderInfoBuilder
import weaver.IOSuite

object MessagePactVerifierWeaverTestSuite extends IOSuite with MessagePactVerifier[IO] {
  val mock = new MockProviderServer(49162)

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
