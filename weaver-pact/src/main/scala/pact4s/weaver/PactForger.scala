package pact4s.weaver

import au.com.dius.pact.consumer.BaseMockServer
import cats.effect.Resource
import cats.implicits._
import pact4s.PactForgerResources
import weaver.MutableFSuite

trait SimplePactForger[F[_]] extends PactForgerResources { self: MutableFSuite[F] =>
  override type Res = BaseMockServer

  private val F = effect

  override def sharedResource: Resource[F, BaseMockServer] = Resource.make[F, BaseMockServer] {
    for {
      _ <- validatePactVersion.fold(F.unit)(F.raiseError)
      _ <- F.delay(server.start())
      _ <- F.delay(server.waitForServer())
    } yield server
  } { s =>
    logger.info(s"Writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}")
    F.delay(s.verifyResultAndWritePact(null, pactTestExecutionContext, pact, mockProviderConfig.getPactVersion)) >>
      F.delay(s.stop())
  }
}

trait PactForger[F[_]] extends PactForgerResources { self: MutableFSuite[F] =>
  type Resources
  override type Res = (Resources, BaseMockServer)
  private val F = effect

  private val serverResource = Resource.make[F, BaseMockServer] {
    for {
      _ <- validatePactVersion.fold(F.unit)(F.raiseError)
      _ <- F.delay(server.start())
      _ <- F.delay(server.waitForServer())
    } yield server
  } { s =>
    logger.info(s"Writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}")
    F.delay(s.verifyResultAndWritePact(null, pactTestExecutionContext, pact, mockProviderConfig.getPactVersion)) >>
      F.delay(s.stop())
  }

  def additionalSharedResource: Resource[F, Resources]

  override def sharedResource: Resource[F, (Resources, BaseMockServer)] = (additionalSharedResource, serverResource).tupled
}
