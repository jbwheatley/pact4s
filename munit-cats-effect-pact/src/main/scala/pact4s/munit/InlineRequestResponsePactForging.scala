package pact4s.munit

import au.com.dius.pact.consumer.{BaseMockServer, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.{IO, Resource}
import cats.syntax.all._
import munit.CatsEffectSuite
import pact4s.{InlineRequestResponsePactResources, RequestResponsePactForgerResources}

trait InlineRequestResponsePactForging extends CatsEffectSuite with InlineRequestResponsePactResources { self =>
  private def serverResource(self: RequestResponsePactForgerResources): Resource[IO, BaseMockServer] = {
    import self._
    val server = createServer
    Resource.make[IO, BaseMockServer] {
      {
        for {
          _ <- validatePactVersion(mockProviderConfig.getPactVersion).liftTo[IO]
          _ <- IO(server.start())
          _ <- IO(server.waitForServer())
        } yield server
      }.onError(_ => IO(server.stop()))
    } { s =>
      IO(s.stop())
    }
  }

  sealed abstract class ForgerImpl extends InlineRequestResponsePactForger {
    def apply(test: BaseMockServer => IO[Unit]): IO[Unit]
  }

  override private[pact4s] type Forger = ForgerImpl

  override def withPact(aPact: RequestResponsePact): Forger =
    new ForgerImpl {
      override val pact: RequestResponsePact                          = aPact
      override val pactTestExecutionContext: PactTestExecutionContext = self.pactTestExecutionContext

      override def apply(test: BaseMockServer => IO[Unit]): IO[Unit] = serverResource(this).use { server =>
        {
          for {
            _ <- test(server)
            _ <- self.beforeWritePacts()
            _ <- verifyResultAndWritePactFiles(server).liftTo[IO]
          } yield ()
        }.onError(_ =>
          IO(
            pact4sLogger.error(
              notWritingPactMessage(pact)
            )
          )
        )
      }
    }

  override private[pact4s] type Effect[_] = IO[_]

  def beforeWritePacts(): IO[Unit] = IO.unit
}
