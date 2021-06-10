package pact4s.scalatest

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.{MockProviderServer, ProviderInfoBuilder}

class RequestResponsePactVerifierScalaTestSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll {
  val mock = new MockProviderServer(3456)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sConsumer",
    providerName = "Pact4sProvider",
    fileName = "./scripts/Pact4sConsumer-Pact4sProvider.json"
  )

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  it should "Verify pacts for provider `Pact4sProvider`" in {
    verifyPacts()
  }
}
