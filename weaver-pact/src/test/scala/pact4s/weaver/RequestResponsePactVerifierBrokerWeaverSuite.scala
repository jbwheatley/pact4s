package pact4s.weaver

import cats.effect.{IO, Resource}
import org.http4s.server.Server
import pact4s.MockProviderServer
import pact4s.provider.{ConsumerVersionSelector, ProviderInfoBuilder, PublishVerificationResults}
import weaver.IOSuite

object RequestResponsePactVerifierBrokerWeaverSuite extends IOSuite with PactVerifier {
  type Res = Server

  val mock = new MockProviderServer(49163)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider", consumerVersionSelector = ConsumerVersionSelector().withMainBranch(true))

  test("Verify pacts for provider `Pact4sProvider`") {
    for {
      a <- IO(
        succeed(
          verifyPacts(
            publishVerificationResults = Some(
              PublishVerificationResults(
                providerVersion = "SNAPSHOT"
              )
            )
          )
        )
      )
      x <- mock.featureXState.tryGet
    } yield a && assert(x.isEmpty)
  }
}
object RequestResponsePactVerifierBrokerFeatureBranchWeaverSuite extends IOSuite with PactVerifier {
  type Res = Server

  val mock = new MockProviderServer(49173)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider", consumerVersionSelector = ConsumerVersionSelector().withBranch("feat/x"))

  test("Verify pacts for provider `Pact4sProvider`") {
    for {
      a <- IO(
        succeed(
          verifyPacts(
            publishVerificationResults = Some(
              PublishVerificationResults(
                providerVersion = "SNAPSHOT"
              )
            )
          )
        )
      )
      x <- mock.featureXState.tryGet
    } yield a && assert(x.contains(true))
  }
}
