package pact4s.scalatest

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.{MockProviderServer, ProviderInfoBuilder}
import pact4s.messages.MessagesProvider

class MessagePactVerifierScalaTestSuite extends AnyFlatSpec with MessagePactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer(isRequestResponse = false)

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
