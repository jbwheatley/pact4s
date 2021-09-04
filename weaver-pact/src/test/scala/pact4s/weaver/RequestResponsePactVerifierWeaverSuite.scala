package pact4s.weaver

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.{MockProviderServer, ProviderInfoBuilder}
import weaver.IOSuite

object RequestResponsePactVerifierWeaverSuite extends IOSuite with PactVerifier {
  type Res = Server

  val mock = new MockProviderServer

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sConsumer",
    providerName = "Pact4sProvider",
    fileName = "./scripts/Pact4sConsumer-Pact4sProvider.json"
  )

  pureTest("Verify pacts for provider `Pact4sProvider`") {
    succeed(verifyPacts())
  }
}
