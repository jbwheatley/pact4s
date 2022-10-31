package pact4s.munit.message

import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.munit.MessagePactVerifier
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}

class PactVerifierBrokerSuite extends CatsEffectSuite with MessagePactVerifier {
  val mock = new MockProviderServer(49152)

  def messages: ResponseFactory              = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.brokerMessageProviderInfo

  verifyPacts(
    publishVerificationResults = Some(
      PublishVerificationResults(
        providerVersion = "SNAPSHOT"
      )
    )
  )
}
