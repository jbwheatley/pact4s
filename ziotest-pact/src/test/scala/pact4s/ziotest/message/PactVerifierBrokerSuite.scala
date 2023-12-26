package pact4s.ziotest.message

import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}
import pact4s.ziotest.MessagePactVerifier
import zio.{Scope, ZIO}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object PactVerifierBrokerSuite extends ZIOSpecDefault with MessagePactVerifier {
  lazy val mock = new MockProviderServer(49156)

  def messages: ResponseFactory     = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.brokerMessageProviderInfo
  override def spec: Spec[TestEnvironment with Scope, Any] =
    test("Verify pacts for provider `MessageProvider`, zio-test")(
      ZIO
        .attempt(
          verifyPacts(publishVerificationResults = Some(PublishVerificationResults(providerVersion = "SNAPSHOT")))
        )
        .as(assertTrue(true))
    )
}
