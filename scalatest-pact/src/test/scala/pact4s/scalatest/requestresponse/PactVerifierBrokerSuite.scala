package pact4s.scalatest.requestresponse

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.MockProviderServer
import pact4s.provider.{ConsumerVersionSelector, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.scalatest.PactVerifier

class PactVerifierBrokerSuite
    extends AnyFlatSpec
    with PactVerifier
    with BeforeAndAfterAll
    with Matchers {
  val mock = new MockProviderServer(49158)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider", consumerVersionSelector = ConsumerVersionSelector().withMainBranch(true))

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit =
    cleanUp.unsafeRunSync()

  it should "Verify pacts for provider `Pact4sProvider`" in {
    verifyPacts(
      publishVerificationResults = Some(
        PublishVerificationResults(
          providerVersion = "SNAPSHOT"
        )
      )
    )
    mock.featureXState.tryGet.unsafeRunSync() shouldBe None
  }
}


