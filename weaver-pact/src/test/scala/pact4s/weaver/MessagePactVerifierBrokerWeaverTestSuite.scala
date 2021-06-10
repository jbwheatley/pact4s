package pact4s.weaver

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
import weaver.IOSuite

object MessagePactVerifierBrokerWeaverTestSuite extends IOSuite with PactVerifier {
  type Res = Server

  val mock = new MockProviderServer(1237)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(
    providerName = "Pact4sMessageProvider",
    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.messages")))
  )

  pureTest("Verify pacts for provider `MessageProvider`") {
    succeed(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT",
            providerTags = Nil
          )
        )
      )
    )
  }

}
