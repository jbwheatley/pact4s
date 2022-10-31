package pact4s.scalatest.requestresponse

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.MockProviderServer
import pact4s.provider.ProviderInfoBuilder
import pact4s.scalatest.PactVerifier

class PactVerifierSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll {
  val mock = new MockProviderServer(49159)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo()

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit =
    cleanUp.unsafeRunSync()

  it should "Verify pacts for provider `Pact4sProvider`" in {
    verifyPacts()
  }
}
