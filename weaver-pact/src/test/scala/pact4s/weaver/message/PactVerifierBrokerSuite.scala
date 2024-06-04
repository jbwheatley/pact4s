package pact4s.weaver.message

import cats.effect.IO
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}
import pact4s.weaver.MessagePactVerifier
import weaver.SimpleIOSuite

object PactVerifierBrokerSuite extends SimpleIOSuite with MessagePactVerifier[IO] {
  val mock = new MockProviderServer(49161)

  def messages: ResponseFactory              = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.brokerMessageProviderInfo

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
