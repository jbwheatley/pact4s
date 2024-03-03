package pact4s.ziotest.requestresponse

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import pact4s.MockProviderServer
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder}
import pact4s.ziotest.PactVerifier
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO, ZLayer}

import scala.concurrent.duration.DurationInt

object PactVerifierBrokerMatchingBranchSuite extends ZIOSpecDefault with PactVerifier {
  val mock = new MockProviderServer(49300, hasFeatureX = true)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(
      consumerVersionSelector = ConsumerVersionSelectors.matchingBranch,
      pendingPactsEnabled = true
    )

  val mockLayer: ZLayer[Any with Scope, Throwable, IO[Unit]] = ZLayer.fromZIO {
    ZIO.acquireRelease(
      for {
        serverStart <- ZIO.attempt(mock.server.allocated.unsafeRunSync())
      } yield serverStart._2
    )(shutdown =>
      ZIO.attempt(shutdown.unsafeRunSync()).catchAll(e => ZIO.logError(s"failed to shutdown mock server: $e"))
    )
  }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    test("Verify pacts for provider `Pact4sProvider` with a feature branch and matching branch selector, zio-test")(
      for {
        _ <- ZIO.attempt(verifyPacts(Some(Branch("feat/x"))))
        featureXState = mock.featureXState.get.unsafeRunTimed(10.seconds)
      } yield assertTrue(featureXState.contains(true))
    ).provideLayerShared(mockLayer)
}
