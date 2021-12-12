package pact4s.munit

import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}

class MessagePactVerifierBrokerMUnitSuite extends CatsEffectSuite with MessagePactVerifier {
  val mock = new MockProviderServer(49152)

  def messages: ResponseFactory              = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(providerName = "Pact4sMessageProvider")

  verifyPacts(
    publishVerificationResults = Some(
      PublishVerificationResults(
        providerVersion = "SNAPSHOT"
      )
    )
  )
}
