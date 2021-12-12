package pact4s.scalatest

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.ProviderInfoBuilder

class MessagePactVerifierScalaTestSuite extends AnyFlatSpec with MessagePactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer(49157)

  def messages: ResponseFactory = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sMessageConsumer",
    providerName = "Pact4sMessageProvider",
    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json"
  )

  it should "Verify pacts for provider `MessageProvider`" in {
    verifyPacts()
  }
}
