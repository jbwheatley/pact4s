package pact4s.weaver.requestresponse

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.MockProviderServer
import pact4s.provider.ProviderInfoBuilder
import pact4s.weaver.PactVerifier
import weaver.IOSuite

object PactVerifierFileSuite extends IOSuite with PactVerifier[IO] {
  type Res = Server

  val mock = new MockProviderServer(49164)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo()

  test("Verify pacts for provider `Pact4sProvider`") {
    verifyPacts().map(succeed)
  }
}
