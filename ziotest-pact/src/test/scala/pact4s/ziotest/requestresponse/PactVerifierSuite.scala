package pact4s.ziotest.requestresponse

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import pact4s.MockProviderServer
import pact4s.provider.ProviderInfoBuilder
import pact4s.ziotest.PactVerifier
import zio.{Scope, ZIO, ZLayer}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object PactVerifierSuite extends ZIOSpecDefault with PactVerifier {
  val mock = new MockProviderServer(49159)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo()

  val mockLayer: ZLayer[Any with Scope, Throwable, IO[Unit]] = ZLayer.fromZIO {
    ZIO.acquireRelease(
      for {
        serverStart <- ZIO.attempt(mock.server.allocated.unsafeRunSync())
        (_, shutdown) = serverStart
      } yield shutdown
    )(shutdown =>
      ZIO.attempt(shutdown.unsafeRunSync()).catchAll(e => ZIO.logError(s"failed to shutdown mock server: $e"))
    )
  }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    test("Verify pacts for provider `Pact4sProvider`, zio-test") {
      ZIO.attempt(verifyPacts()).as(assertTrue(true))
    }.provideLayerShared(mockLayer)
}
