package pact4s.scalatest.requestresponse

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.MockProviderServer
import pact4s.provider.{ProviderInfoBuilder, ProviderState}
import pact4s.scalatest.PactVerifier

class PactVerifierStateChangeFunctionSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll {
  val mock = new MockProviderServer(49171)

  override val provider: ProviderInfoBuilder = mock
    .fileSourceProviderInfo(
      consumerName = "Pact4sConsumer",
      providerName = "Pact4sProvider",
      fileName = "./scripts/Pact4sConsumer-Pact4sProvider.json"
    )
    .withStateChangeFunction({ case ProviderState("bob exists", params) =>
      val _ = params.getOrElse("foo", fail())
      mock.stateRef.set(Some("bob")).unsafeRunSync()
    }: PartialFunction[ProviderState, Unit])
    .withStateChangeFunctionConfigOverrides(_.withOverrides(portOverride = 64645))

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
