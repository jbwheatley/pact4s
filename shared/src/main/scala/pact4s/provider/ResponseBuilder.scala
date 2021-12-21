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
package provider

import au.com.dius.pact.provider.MessageAndMetadata
import pact4s.algebras.PactBodyJsonEncoder

import java.nio.charset.Charset
import scala.jdk.CollectionConverters._

sealed trait ResponseBuilder {
  def build: AnyRef
}

object ResponseBuilder {
  final case class MessageAndMetadataBuilder(
      message: Array[Byte],
      metadata: Map[String, Any]
  ) extends ResponseBuilder {
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

  final case class ProviderResponseBuilder(
      statusCode: Int,
      contentType: Option[String],
      headers: Map[String, List[String]],
      data: Option[String]
  ) extends ResponseBuilder {
    def build: java.util.Map[String, Any] = Map[String, Any](
      "statusCode"  -> statusCode,
      "contentType" -> contentType.orNull,
      "headers"     -> headers.map { case (k, v) => k -> v.asJava }.asJava,
      "data"        -> data.orNull
    ).asJava

    def withContentType(contentType: String): ProviderResponseBuilder =
      this.copy(contentType = Some(contentType))

    def withHeaders(headers: Map[String, List[String]]): ProviderResponseBuilder = this.copy(headers = headers)
  }

  object ProviderResponseBuilder {
    def apply(statusCode: Int): ProviderResponseBuilder =
      ProviderResponseBuilder(
        statusCode,
        contentType = Some("application/json"),
        headers = Map.empty[String, List[String]],
        data = None
      )

    def apply(statusCode: Int, data: String): ProviderResponseBuilder =
      ProviderResponseBuilder(
        statusCode,
        contentType = Some("application/json"),
        headers = Map.empty[String, List[String]],
        data = Some(data)
      )

    def apply[A: PactBodyJsonEncoder](statusCode: Int, data: A): ProviderResponseBuilder =
      ProviderResponseBuilder(
        statusCode,
        contentType = Some("application/json"),
        headers = Map.empty[String, List[String]],
        data = Some(PactBodyJsonEncoder[A].toJsonString(data))
      )
  }
}
