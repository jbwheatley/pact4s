package pact4s.scalatest.requestresponse

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.MockProviderServer
import pact4s.provider.{Branch, ConsumerVersionSelector, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.scalatest.PactVerifier

import scala.concurrent.duration._

class PactVerifierBrokerMatchingBranchSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll with Matchers {
  val mock = new MockProviderServer(49300, hasFeatureX = true)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(
      "Pact4sProvider",
      consumerVersionSelector = ConsumerVersionSelector().withMatchingBranch
    )

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit =
    cleanUp.unsafeRunSync()

  it should "Verify pacts for provider `Pact4sProvider` with a feature branch and matching branch selector, scalatest" in {
    verifyPacts(
      publishVerificationResults = Some(
        PublishVerificationResults(
          providerVersion = "SNAPSHOT",
          providerBranch = Branch("feat/x")
        )
      )
    )
    mock.featureXState.get.unsafeRunTimed(10.seconds) shouldBe Some(true)
  }
}
