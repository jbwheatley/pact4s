package pact4s.scalatest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}

class MessagePactVerifierBrokerScalaTestSuite extends AnyFlatSpec with MessagePactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer(isRequestResponse = false)

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
