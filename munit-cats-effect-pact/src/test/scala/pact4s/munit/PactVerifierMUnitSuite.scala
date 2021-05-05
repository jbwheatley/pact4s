package pact4s.munit

import pact4s.{MockProviderServer, ProviderInfoBuilder}

class PactVerifierMUnitSuite extends PactVerifier {
  val mock = new MockProviderServer(2345)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo.copy(publishResults = true)

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  verifyPacts()
}
