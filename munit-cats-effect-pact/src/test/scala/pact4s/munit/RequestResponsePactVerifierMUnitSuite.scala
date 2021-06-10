package pact4s.munit

import pact4s.{MockProviderServer, ProviderInfoBuilder}

class RequestResponsePactVerifierMUnitSuite extends PactVerifier {
  val mock = new MockProviderServer(2345)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sConsumer",
    providerName = "Pact4sProvider",
    fileName = "./scripts/Pact4sConsumer-Pact4sProvider.json"
  )

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  verifyPacts()
}
