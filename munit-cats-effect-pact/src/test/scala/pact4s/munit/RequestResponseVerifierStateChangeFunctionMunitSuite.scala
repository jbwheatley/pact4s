package pact4s.munit

import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.provider.{ProviderInfoBuilder, ProviderState}

class RequestResponseVerifierStateChangeFunctionMunitSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49172)

  override val provider: ProviderInfoBuilder = mock
    .fileSourceProviderInfo(
      consumerName = "Pact4sConsumer",
      providerName = "Pact4sProvider",
      fileName = "./scripts/Pact4sConsumer-Pact4sProvider.json"
    )
    .withStateChangeFunction({ case ProviderState("bob exists", params) =>
      val _ = params.getOrElse("foo", fail("params missing value foo"))
      mock.stateRef.set(Some("bob")).unsafeRunSync()
    }: PartialFunction[ProviderState, Unit])
    .withStateChangeFunctionConfigOverrides(_.withOverrides(portOverride = 64643))

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    verifyPacts()
  }
}
