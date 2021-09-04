package pact4s.munit

import munit.CatsEffectSuite
import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.messages.MessagesProvider

class MessagePactVerifierBrokerMUnitSuite extends CatsEffectSuite with MessagePactVerifier {
  val mock = new MockProviderServer(isRequestResponse = false)

  def messages: ResponseFactory              = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(providerName = "Pact4sMessageProvider")

  verifyPacts(
    publishVerificationResults = Some(
      PublishVerificationResults(
        providerVersion = "SNAPSHOT",
        providerTags = Nil
      )
    )
  )
}
