package pact4s.weaver.requestresponse

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.MockProviderServer
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder}
import pact4s.weaver.PactVerifier
import weaver.IOSuite

object PactVerifierBrokerFeatureBranchSuite extends IOSuite with PactVerifier[IO] {
  type Res = Server

  val mock = new MockProviderServer(49173, hasFeatureX = true)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(consumerVersionSelector = ConsumerVersionSelectors.branch("feat/x"))

  test("Verify pacts for provider `Pact4sProvider` with a feature branch, weaver") {
    for {
      a <-
        verifyPacts(
          Some(
            Branch("feat/x")
          )
        ).map(succeed)
      x <- mock.featureXState.tryGet
    } yield a && assert(x.contains(true))
  }
}
