/*
 * Copyright 2021 io.github.jbwheatley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pact4s
package syntax

import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.dsl.{DslPart, MessageContentsBuilder, MetadataBuilder, SynchronousMessagePactBuilder}
import au.com.dius.pact.core.model.messaging.{Message, MessagePact}
import pact4s.algebras.{MessagePactDecoder, PactDslJsonBodyEncoder}
import pact4s.syntax.JavaHelpers.convertJ
import pact4s.syntax.MessagePactOps.{
  MessageContentsBuilderOps,
  MessageOps,
  MessagePactBuilderOps,
  SynchronousMessagePactBuilderOps
}

import scala.jdk.CollectionConverters._

object MessagePactOps {
  class MessagePactBuilderOps(val builder: MessagePactBuilder) extends AnyVal {
    def withMetadata(metadata: Map[String, Any]): MessagePactBuilder = builder.withMetadata(convertJ(metadata))

    def `given`(state: String, params: Map[String, Any]): MessagePactBuilder = builder.`given`(state, convertJ(params))

    def withContent[A](content: A)(implicit ev: PactDslJsonBodyEncoder[A]): MessagePactBuilder =
      builder.withContent(ev.toPactDslJsonBody(content))

    def toMessagePact: MessagePact = builder.toPact[MessagePact]
  }

  class MessageOps(val message: Message) extends AnyVal {
    def as[A](implicit decoder: MessagePactDecoder[A]): Either[Throwable, A] = decoder.decode(message)

    def metadata: Map[String, Any] = message.getMetadata.asScala.toMap
  }

  class SynchronousMessagePactBuilderOps(val builder: SynchronousMessagePactBuilder) extends AnyVal {
    def `given`(state: String, params: Map[String, Any]): SynchronousMessagePactBuilder =
      builder.`given`(state, convertJ(params))

    /** e.g. builder.withRequest(_.withMetadata(Map("foo" -> "bar").withContent("123")) Alternatively use
      * builder.withRequestMetadata(Map("foo" -> "bar")).withRequestContent("123")
      */
    def withRequest(updateMessage: MessageContentsBuilder => MessageContentsBuilder): SynchronousMessagePactBuilder =
      builder.withRequest(content => updateMessage.andThen(_ => ()).apply(content))

    def withRequestContent[A](content: A)(implicit ev: PactDslJsonBodyEncoder[A]): SynchronousMessagePactBuilder =
      withRequestContent(ev.toPactDslJsonBody(content))

    def withRequestContent(body: DslPart): SynchronousMessagePactBuilder = withRequest(_.withContent(body))

    def withRequestMetadata(params: Map[String, Any]): SynchronousMessagePactBuilder =
      withRequest(r => new MessageContentsBuilderOps(r).withMetadata(params))

    def withRequestMetadata(updateMetadata: MetadataBuilder => MetadataBuilder): SynchronousMessagePactBuilder =
      withRequest(r => new MessageContentsBuilderOps(r).withMetadata(updateMetadata))

    /** e.g. builder.withResponse(_.withMetadata(Map("foo" -> "bar").withContent("123")) Alternatively use
      * builder.withResponseMetadata(Map("foo" -> "bar")).withResponseContent("123")
      */
    def withResponse(updateMessage: MessageContentsBuilder => MessageContentsBuilder): SynchronousMessagePactBuilder =
      builder.withResponse(content => updateMessage.andThen(_ => ()).apply(content))

    def withResponseContent[A](content: A)(implicit ev: PactDslJsonBodyEncoder[A]): SynchronousMessagePactBuilder =
      withResponseContent(ev.toPactDslJsonBody(content))

    def withResponseContent(body: DslPart): SynchronousMessagePactBuilder = withResponse(_.withContent(body))

    def withResponseMetadata(params: Map[String, Any]): SynchronousMessagePactBuilder =
      withResponse(r => new MessageContentsBuilderOps(r).withMetadata(params))

    def withResponseMetadata(updateMetadata: MetadataBuilder => MetadataBuilder): SynchronousMessagePactBuilder =
      withResponse(r => new MessageContentsBuilderOps(r).withMetadata(updateMetadata))
  }

  class MessageContentsBuilderOps(val builder: MessageContentsBuilder) extends AnyVal {
    def withMetadata(metadata: Map[String, Any]): MessageContentsBuilder = builder.withMetadata(convertJ(metadata))

    def withMetadata(updateMetadata: MetadataBuilder => MetadataBuilder): MessageContentsBuilder =
      builder.withMetadata(metadata => updateMetadata.andThen(_ => ()).apply(metadata))

    def withContent[A](content: A)(implicit ev: PactDslJsonBodyEncoder[A]): MessageContentsBuilder =
      builder.withContent(ev.toPactDslJsonBody(content))
  }
}

trait MessagePactOps {
  implicit def toMessagePactBuilderOps(builder: MessagePactBuilder): MessagePactBuilderOps = new MessagePactBuilderOps(
    builder
  )

  implicit def toMessageOps(message: Message): MessageOps = new MessageOps(message)

  implicit def toSynchronousMessagePactBuilderOps(
      builder: SynchronousMessagePactBuilder
  ): SynchronousMessagePactBuilderOps = new SynchronousMessagePactBuilderOps(builder)

  implicit def toMessageContentsBuilderOps(builder: MessageContentsBuilder): MessageContentsBuilderOps =
    new MessageContentsBuilderOps(builder)

  sealed class Pact4sMessagePactBuilder() {
    def consumer(consumer: String): MessagePactBuilder = new MessagePactBuilder().consumer(consumer)
  }

  object Pact4sMessagePactBuilder {
    def apply(): Pact4sMessagePactBuilder = new Pact4sMessagePactBuilder()
  }
}
