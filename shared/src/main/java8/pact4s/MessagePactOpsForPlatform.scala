/*
 * Copyright 2021-2021 io.github.jbwheatley
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

import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.core.model.messaging.{Message, MessagePact}

import scala.jdk.CollectionConverters._

object MessagePactOpsForPlatform {
  class MessagePactBuilderOpsForPlatform(val builder: MessagePactBuilder) extends AnyVal {
    def toMessagePact: MessagePact = builder.toPact
  }

  class MessageOpsForPlatform(val message: Message) extends AnyVal {
    def metadata: Map[String, Any] = message.getMetaData.asScala.toMap
  }

  protected[pact4s] trait MessagePactBuilderForPlatform {
    def consumer(consumer: String): MessagePactBuilder = MessagePactBuilder.consumer(consumer)
  }
}
