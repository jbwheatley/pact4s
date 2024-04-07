package pact4s.ziotest.message

import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.ProviderInfoBuilder
import pact4s.ziotest.MessagePactVerifier
import zio.{Scope, ZIO}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object PactVerifierFileSuite extends ZIOSpecDefault with MessagePactVerifier {
  lazy val mock = new MockProviderServer(49157)

  def messages: ResponseFactory     = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.fileSourceMessageProviderInfo

  override def spec: Spec[TestEnvironment with Scope, Any] =
    test("Verify pacts for provider `MessageProvider`, zio-test") {
      ZIO.attempt(verifyPacts()).as(assertTrue(true))
    }
}
