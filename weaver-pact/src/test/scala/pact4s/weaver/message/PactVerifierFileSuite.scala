package pact4s.weaver.message

import cats.effect.IO
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.ProviderInfoBuilder
import pact4s.weaver.MessagePactVerifier
import weaver.SimpleIOSuite

object PactVerifierFileSuite extends SimpleIOSuite with MessagePactVerifier[IO] {
  val mock = new MockProviderServer(49162)

  def messages: ResponseFactory              = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.fileSourceMessageProviderInfo

  pureTest("Verify pacts for provider `MessageProvider`") {
    succeed(verifyPacts())
  }
}
