package pact4s.weaver

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
import weaver.IOSuite

object RequestResponsePactVerifierBrokerWeaverSuite extends IOSuite with PactVerifier {
  type Res = Server

  val mock = new MockProviderServer(1236)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo("Pact4sProvider")

  test("Verify pacts for provider `Pact4sProvider`") {
    IO(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT",
            providerTags = Nil
          )
        )
      )
    ).map(succeed)
  }
}
