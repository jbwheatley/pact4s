package pact4s.munit.message

import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.munit.MessagePactVerifier
import pact4s.provider.ProviderInfoBuilder

class PactVerifierFileSuite extends CatsEffectSuite with MessagePactVerifier {
  val mock = new MockProviderServer(49153)

  def messages: ResponseFactory = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sMessageConsumer",
    providerName = "Pact4sMessageProvider",
    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json"
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    verifyPacts()
  }
}
