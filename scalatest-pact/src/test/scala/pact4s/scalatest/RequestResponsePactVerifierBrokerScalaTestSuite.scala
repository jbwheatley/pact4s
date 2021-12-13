package pact4s.scalatest

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.MockProviderServer
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}

class RequestResponsePactVerifierBrokerScalaTestSuite extends AnyFlatSpec with PactVerifier {
  val mock = new MockProviderServer(49158)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider")

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit = {
    super.afterAll()
    cleanUp.unsafeRunSync()
  }

  it should "Verify pacts for provider `Pact4sProvider`" in {
    verifyPacts(
      publishVerificationResults = Some(
        PublishVerificationResults(
          providerVersion = "SNAPSHOT"
        )
      )
    )
  }
}
