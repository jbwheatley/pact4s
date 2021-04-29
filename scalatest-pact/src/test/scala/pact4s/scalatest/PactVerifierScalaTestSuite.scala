package pact4s.scalatest

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import pact4s.{MockProviderServer, ProviderInfoBuilder}

class PactVerifierScalaTestSuite extends PactVerifier with BeforeAndAfterAll {
  val mock = new MockProviderServer(3456)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  verifyPacts()
}
