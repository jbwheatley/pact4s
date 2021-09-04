package pact4s.scalatest

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}

class RequestResponsePactVerifierBrokerScalaTestSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll {
  val mock = new MockProviderServer

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo("Pact4sProvider")

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  it should "Verify pacts for provider `Pact4sProvider`" in {
    verifyPacts(
      publishVerificationResults = Some(
        PublishVerificationResults(
          providerVersion = "SNAPSHOT",
          providerTags = Nil
        )
      )
    )
  }
}
