package pact4s.scalatest

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import pact4s.{MockProviderServer, ProviderInfoBuilder, VerificationType}

class MessagePactVerifierScalaTestSuite extends PactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer(3458)

  def provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    "Pact4sMessageConsumer",
    "Pact4sMessageProvider",
    "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json",
    VerificationType.AnnotatedMethod
  )

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  verifyPacts()
}
