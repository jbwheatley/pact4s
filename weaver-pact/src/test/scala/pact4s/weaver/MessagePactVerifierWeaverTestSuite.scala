package pact4s.weaver

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.{MockProviderServer, ProviderInfoBuilder}
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
import weaver.IOSuite

object MessagePactVerifierWeaverTestSuite extends IOSuite with PactVerifier {
  type Res = Server

  val mock = new MockProviderServer(1236)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sMessageConsumer",
    providerName = "Pact4sMessageProvider",
    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json",
    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.messages")))
  )

  pureTest("Verify pacts for provider `MessageProvider`") {
    succeed(verifyPacts())
  }
}
