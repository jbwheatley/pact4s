package pact4s.weaver

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}
import pact4s.MockProviderServer
import weaver.IOSuite

object RequestResponsePactVerifierBrokerWeaverSuite extends IOSuite with PactVerifierWithResources[IO] {
  type Resources = Server

  val mock = new MockProviderServer(49163)

  override def additionalSharedResource: Resource[IO, Server] = mock.server

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
