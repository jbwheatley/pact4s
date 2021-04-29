package pact4s.weaver

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.{MockProviderServer, ProviderInfoBuilder}
import weaver.IOSuite

object PactVeriferWeaverSuite extends IOSuite with PactVerifier[IO] {
  type Res = Server

  val mock = new MockProviderServer(1234)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo

  verifyPacts()
}
