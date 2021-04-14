package pact4s.scalatest

import au.com.dius.pact.provider.ProviderInfo
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import pact4s.MockProviderServer

class PactVerifierScalaTestSuite extends PactVerifier with BeforeAndAfterAll {
  val mock = new MockProviderServer(3456)

  override val provider: ProviderInfo = mock.providerInfo

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  verifyPacts()
}
