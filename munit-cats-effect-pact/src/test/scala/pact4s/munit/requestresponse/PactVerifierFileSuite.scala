package pact4s.munit.requestresponse

import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.munit.PactVerifier
import pact4s.provider.ProviderInfoBuilder

class PactVerifierFileSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49155)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo()

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
