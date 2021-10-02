package pact4s.weaver

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
import weaver.IOSuite

object RequestResponsePactVerifierBrokerWeaverSuite extends IOSuite with PactVerifier {
  type Res = Server

  val mock = new MockProviderServer

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo("Pact4sProvider")

  pureTest("Verify pacts for provider `Pact4sProvider`") {
    succeed(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT"
          )
        )
      )
    )
  }
}
