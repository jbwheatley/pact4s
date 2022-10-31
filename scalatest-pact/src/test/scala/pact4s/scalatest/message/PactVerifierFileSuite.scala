package pact4s.scalatest.message

import org.scalatest.flatspec.AnyFlatSpec
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.ProviderInfoBuilder
import pact4s.scalatest.MessagePactVerifier

class PactVerifierFileSuite extends AnyFlatSpec with MessagePactVerifier {
  lazy val mock = new MockProviderServer(49157)

  def messages: ResponseFactory     = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.fileSourceMessageProviderInfo

  it should "Verify pacts for provider `MessageProvider`" in {
    verifyPacts()
  }
}
