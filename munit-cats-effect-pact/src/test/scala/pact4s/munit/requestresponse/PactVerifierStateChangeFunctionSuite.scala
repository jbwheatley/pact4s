package pact4s.munit.requestresponse

import cats.effect.IO
import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.munit.PactVerifier
import pact4s.provider.ProviderInfoBuilder

class PactVerifierStateChangeFunctionSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49172)

  override val provider: ProviderInfoBuilder = mock
    .fileSourceProviderInfo(
      useStateChangeFunction = true,
      stateChangePortOverride = Some(64643)
    )

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    IO(verifyPacts())
  }
}
