package pact4s.munit
import cats.effect.IO
import munit.CatsEffectSuite
import pact4s.provider.{ConsumerVersionSelector, ProviderInfoBuilder, PublishVerificationResults}
import pact4s.MockProviderServer
import pact4s.provider.ProviderVerificationOption.{SHOW_FULLDIFF, SHOW_STACKTRACE}

class RequestResponsePactVerifierBrokerMUnitSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49154)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider", consumerVersionSelector = ConsumerVersionSelector().withMainBranch(true))

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    IO(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT"
          )
        )
      )
    ) *> mock.featureXState.tryGet.assertEquals(None)
  }
}

class RequestResponsePactVerifierBrokerFeatureBranchMUnitSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49164, hasFeatureX = true)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider", consumerVersionSelector = ConsumerVersionSelector().withBranch("feat/x"))

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    IO(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT"
          )
        ),
        providerVerificationOptions = SHOW_FULLDIFF :: SHOW_STACKTRACE :: Nil
      )
    ) *> mock.featureXState.tryGet.assertEquals(Some(true))
  }
}
