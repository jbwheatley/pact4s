package pact4s.munit

import munit.CatsEffectSuite
import pact4s.{MockProviderServer, ProviderInfoBuilder}
import pact4s.messages.MessagesProvider

class MessagePactVerifierMUnitSuite extends CatsEffectSuite with MessagePactVerifier {
  val mock = new MockProviderServer

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
