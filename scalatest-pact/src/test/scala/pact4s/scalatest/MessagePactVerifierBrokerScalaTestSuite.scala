package pact4s.scalatest
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}

class MessagePactVerifierBrokerScalaTestSuite extends AnyFlatSpec with MessagePactVerifier {
  lazy val mock = new MockProviderServer(49156)

  def messages: ResponseFactory     = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.brokerProviderInfo(providerName = "Pact4sMessageProvider")

  it should "Verify pacts for provider `MessageProvider`" in {
    verifyPacts(
      publishVerificationResults = Some(
        PublishVerificationResults(
          providerVersion = "SNAPSHOT"
        )
      )
    )
  }
}
