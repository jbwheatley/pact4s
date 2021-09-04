package pact4s.scalatest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.messages.MessagesProvider

class MessagePactVerifierBrokerScalaTestSuite extends AnyFlatSpec with MessagePactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer

  def messages: ResponseFactory     = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.brokerProviderInfo(providerName = "Pact4sMessageProvider")

  it should "Verify pacts for provider `MessageProvider`" in {
    verifyPacts(
      publishVerificationResults = Some(
        PublishVerificationResults(
          providerVersion = "SNAPSHOT",
          providerTags = Nil
        )
      )
    )
  }
}
