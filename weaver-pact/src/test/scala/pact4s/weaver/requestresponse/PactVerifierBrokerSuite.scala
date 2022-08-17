package pact4s.weaver.requestresponse

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.MockProviderServer
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder}
import pact4s.weaver.PactVerifier
import weaver.IOSuite

object PactVerifierBrokerSuite extends IOSuite with PactVerifier {
  type Res = Server

  val mock = new MockProviderServer(49163)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider", consumerVersionSelector = ConsumerVersionSelectors.mainBranch)

  test("Verify pacts for provider `Pact4sProvider`, weaver") {
    for {
      a <- IO(
        succeed(
          verifyPacts(
            Some(Branch.MAIN)
          )
        )
      )
      x <- mock.featureXState.tryGet
    } yield a && assert(x.isEmpty)
  }
}
