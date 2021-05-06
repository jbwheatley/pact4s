package pact4s.weaver

import au.com.dius.pact.core.model.messaging.Message
import cats.effect.{Ref, Resource}
import cats.implicits._
import pact4s.MessagePactForgerResources
import weaver.MutableFSuite

import scala.jdk.CollectionConverters.ListHasAsScala

trait SimpleMessagePactForger[F[_]] extends WeaverMessagePactForgerBase[F] {
  override type Res = List[Message]
  override def sharedResource: Resource[F, List[Message]] = messagesResource
}

trait MessagePactForger[F[_]] extends WeaverMessagePactForgerBase[F] {
  type Resources
  override type Res = (Resources, List[Message])

  def additionalSharedResource: Resource[F, Resources]

  override def sharedResource: Resource[F, (Resources, List[Message])] =
    (additionalSharedResource, messagesResource).tupled
}

trait WeaverMessagePactForgerBase[F[_]] extends MutableFSuite[F] with MessagePactForgerResources {
  private val F                           = effect
  private val testFailed: Ref[F, Boolean] = Ref.unsafe(false)

  private[weaver] val messagesResource: Resource[F, List[Message]] = Resource.make[F, List[Message]] {
    validatePactVersion(pactSpecVersion).fold(F.unit)(F.raiseError).as {
      pact.getMessages.asScala.toList
    }
  } { _ =>
    testFailed.get.flatMap {
      case true =>
        logger.info(
          s"Not writing message pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to file because tests failed."
        )
        F.unit
      case false =>
        logger.info(
          s"Writing message pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}"
        )
        F.delay(pact.write(pactTestExecutionContext.getPactFolder, pactSpecVersion)).flatMap { a =>
          Option(a.component2()).traverse_(F.raiseError)
        }
    }
  }
}
