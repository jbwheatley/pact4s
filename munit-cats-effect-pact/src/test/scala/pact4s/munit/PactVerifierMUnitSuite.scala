package pact4s.munit

import au.com.dius.pact.provider.ProviderInfo
import pact4s.MockProviderServer

class PactVerifierMUnitSuite extends PactVerifier {
  val mock = new MockProviderServer(2345)

  override val provider: ProviderInfo = mock.providerInfo

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  verifyPacts()
}
