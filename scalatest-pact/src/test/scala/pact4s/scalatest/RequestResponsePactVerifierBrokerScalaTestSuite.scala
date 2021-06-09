package pact4s.scalatest

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults, VerificationType}

class RequestResponsePactVerifierBrokerScalaTestSuite extends PactVerifier with BeforeAndAfterAll {
  val mock = new MockProviderServer(3457)

  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(
    providerName = "Pact4sProvider",
    verificationType = VerificationType.RequestResponse
  )

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  verifyPacts(
    publishVerificationResults = Some(
      PublishVerificationResults(
        providerVersion = "SNAPSHOT",
        providerTags = Nil
      )
    )
  )
}
