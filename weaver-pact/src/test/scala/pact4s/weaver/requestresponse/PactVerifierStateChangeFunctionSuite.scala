package pact4s.weaver.requestresponse

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.MockProviderServer
import pact4s.provider.{ProviderInfoBuilder, ProviderState}
import pact4s.weaver.PactVerifier
import weaver.IOSuite

object PactVerifierStateChangeFunctionSuite extends IOSuite with PactVerifier {
  override type Res = Server

  val mock = new MockProviderServer(49170)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock
    .fileSourceProviderInfo(
      consumerName = "Pact4sConsumer",
      providerName = "Pact4sProvider",
      fileName = "./scripts/Pact4sConsumer-Pact4sProvider.json"
    )
    .withStateChangeFunction({ case ProviderState("bob exists", params) =>
      val _ = params.getOrElse("foo", fail("params missing value foo"))
      mock.stateRef.set(Some("bob")).unsafeRunSync()
    }: PartialFunction[ProviderState, Unit])
    .withStateChangeFunctionConfigOverrides(_.withOverrides(portOverride = 64640))

  pureTest("Verify pacts for provider `Pact4sProvider`") {
    succeed(verifyPacts())
  }
}
