package pact4s.weaver

import au.com.dius.pact.consumer.{BaseMockServer, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.Resource
import cats.syntax.all._
import pact4s.{InlineRequestResponsePactResources, RequestResponsePactForgerResources}
import weaver.{Expectations, MutableFSuite}

trait InlineRequestResponsePactForging[F[_]] extends MutableFSuite[F] with InlineRequestResponsePactResources { self =>
  private val F = effect
  private[weaver] def serverResource(self: RequestResponsePactForgerResources): Resource[F, BaseMockServer] = {
    import self._
    val server = createServer
    Resource.make[F, BaseMockServer] {
      {
        for {
          _ <- validatePactVersion(mockProviderConfig.getPactVersion).liftTo[F]
          _ <- F.delay(server.start())
          _ <- F.delay(server.waitForServer())
        } yield server
      }.onError(_ => F.delay(server.stop()))
    } { s =>
      F.delay(s.stop())
    }
  }

  sealed abstract class ForgerImpl extends InlineRequestResponsePactForger {
    def apply(test: BaseMockServer => F[Expectations]): F[Expectations]
  }

  override private[pact4s] type Forger = ForgerImpl

  override def withPact(aPact: RequestResponsePact): Forger =
    new ForgerImpl {
      override val pact: RequestResponsePact                          = aPact
      override val pactTestExecutionContext: PactTestExecutionContext = self.pactTestExecutionContext

      override def apply(test: BaseMockServer => F[Expectations]): F[Expectations] = serverResource(this).use {
        server =>
          {
            for {
              res <- test(server)
              _   <- self.beforeWritePacts()
              _   <- verifyResultAndWritePactFiles(server).liftTo[F]
            } yield res
          }.onError(_ =>
            F.delay(
              pact4sLogger.error(
                notWritingPactMessage(pact)
              )
            )
          )
      }
    }

  override private[pact4s] type Effect[_] = F[_]

  def beforeWritePacts(): F[Unit] = F.unit
}
