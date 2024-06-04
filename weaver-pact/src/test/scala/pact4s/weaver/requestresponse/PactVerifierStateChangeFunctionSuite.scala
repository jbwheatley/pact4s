package pact4s.weaver.requestresponse

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.MockProviderServer
import pact4s.provider.ProviderInfoBuilder
import pact4s.weaver.PactVerifier
import weaver.IOSuite

object PactVerifierStateChangeFunctionSuite extends IOSuite with PactVerifier[IO] {
  override type Res = Server

  val mock = new MockProviderServer(49170)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock
    .fileSourceProviderInfo(
      consumerName = "Pact4sConsumer",
      providerName = "Pact4sProvider",
      fileName = "./scripts/Pact4sConsumer-Pact4sProvider.json",
      useStateChangeFunction = true,
      stateChangePortOverride = Some(64640)
    )

  test("Verify pacts for provider `Pact4sProvider`") {
    verifyPacts().map(succeed)
  }
}
