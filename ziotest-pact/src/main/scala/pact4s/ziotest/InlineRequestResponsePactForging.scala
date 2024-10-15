package pact4s.ziotest

import au.com.dius.pact.consumer.{BaseMockServer, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import pact4s.{InlineRequestResponsePactResources, RequestResponsePactForgerResources}
import zio.test.{TestResult, ZIOSpecDefault}
import zio._

trait InlineRequestResponsePactForging extends ZIOSpecDefault with InlineRequestResponsePactResources { self =>
  private def serverResource(
      self: RequestResponsePactForgerResources
  ): RIO[Scope, BaseMockServer] = {
    import self._
    val server = createServer
    ZIO.acquireRelease {
      for {
        _ <- ZIO.fromEither(validatePactVersion(mockProviderConfig.getPactVersion))
        _ <- ZIO.attempt(server.start())
        _ <- ZIO.attempt(server.waitForServer())
      } yield server
    } { s =>
      ZIO.attempt(s.stop()).catchAll(e => ZIO.logError(s"failed to stop mock server: $e")).as(())
    }
  }

  sealed abstract class ForgerImpl extends InlineRequestResponsePactForger {
    def apply(test: BaseMockServer => Task[TestResult]): Task[TestResult]
  }

  override private[pact4s] type Forger = ForgerImpl

  override def withPact(aPact: RequestResponsePact): Forger =
    new ForgerImpl {
      override val pact: RequestResponsePact                          = aPact
      override val pactTestExecutionContext: PactTestExecutionContext = self.pactTestExecutionContext

      override def apply(test: BaseMockServer => Task[TestResult]): Task[TestResult] = ZIO.scoped {
        serverResource(this).flatMap { server =>
          {
            for {
              res <- test(server)
              _   <- self.beforeWritePacts()
              _   <- ZIO.fromEither(verifyResultAndWritePactFiles(server))
            } yield res
          }.onError(e =>
            ZIO.scoped(
              ZIO.attempt(
                pact4sLogger.error(e.squashWith(identity))(
                  notWritingPactMessage(pact)
                )
              )
            )
          )
        }
      }
    }

  override private[pact4s] type Effect[_] = UIO[_]

  def beforeWritePacts(): UIO[Unit] = ZIO.unit
}
