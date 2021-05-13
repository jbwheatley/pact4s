package pact4s

import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.core.model.messaging.Message
import pact4s.MessagePactOps.{MessageOps, MessagePactBuilderOps}

import scala.jdk.CollectionConverters.{MapHasAsJava, MapHasAsScala}

object MessagePactOps {
  class MessagePactBuilderOps(val builder: MessagePactBuilder) extends AnyVal {
    def withMetadata(metadata: Map[String, Any]): MessagePactBuilder = builder.withMetadata(metadata.asJava)

    def withContent[A](content: A)(implicit ev: PactDslJsonBodyEncoder[A]): MessagePactBuilder =
      builder.withContent(ev.toPactDslJsonBody(content))
  }

  class MessageOps(val message: Message) extends AnyVal {
    def as[A](implicit decoder: MessagePactDecoder[A]): Either[Throwable, A] = decoder.decode(message)
    def metadata: Map[String, Any]                                           = message.getMetaData.asScala.toMap
  }
}

trait MessagePactOps {
  implicit def toMessagePactBuilderOps(builder: MessagePactBuilder): MessagePactBuilderOps = new MessagePactBuilderOps(
    builder
  )

  implicit def toMessageOps(message: Message): MessageOps = new MessageOps(message)
}
