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

import au.com.dius.pact.provider.MessageAndMetadata

import java.nio.charset.Charset
import scala.jdk.CollectionConverters._

class MessageAndMetadataBuilder(
    val message: Array[Byte],
    val metadata: Map[String, Any]
) {
  def build: MessageAndMetadata = new MessageAndMetadata(message, metadata.asJava)
}

object MessageAndMetadataBuilder {
  def apply(message: String, charset: Charset, metadata: Map[String, Any]): MessageAndMetadataBuilder =
    new MessageAndMetadataBuilder(message.getBytes(charset), metadata)

  def apply(message: String, metadata: Map[String, Any]): MessageAndMetadataBuilder =
    new MessageAndMetadataBuilder(message.getBytes, metadata)

  def apply[A: PactBodyJsonEncoder](message: A, metadata: Map[String, Any]): MessageAndMetadataBuilder =
    new MessageAndMetadataBuilder(PactBodyJsonEncoder[A].toJsonString(message).getBytes, metadata)

  def apply(message: String): MessageAndMetadataBuilder = apply(message, Map.empty[String, Any])

  def apply[A: PactBodyJsonEncoder](message: A): MessageAndMetadataBuilder = apply(message, Map.empty[String, Any])
}
