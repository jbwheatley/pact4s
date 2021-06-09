package pact4s.munit

import pact4s.{MockProviderServer, ProviderInfoBuilder, VerificationType}

class MessagePactVerifierBrokerMUnitSuite extends PactVerifier {
  val mock = new MockProviderServer(2347)

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(
    providerName = "Pact4sMessageProvider",
    verificationType = VerificationType.AnnotatedMethod
  )

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  verifyPacts()
}
