package pact4s

import au.com.dius.pact.provider.MessageAndMetadata

import java.nio.charset.Charset
import scala.jdk.CollectionConverters._

sealed trait ResponseBuilder {
  def build: AnyRef
}

sealed case class MessageAndMetadataBuilder(
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

sealed case class ProviderResponseBuilder(
    statusCode: Int,
    contentType: Option[String],
    headers: Map[String, List[String]],
    data: Option[String]
) extends ResponseBuilder {
  def build: java.util.Map[String, Any] = Map[String, Any](
    "statusCode"  -> statusCode,
    "contentType" -> contentType.orNull,
    "headers"     -> headers.asJava,
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
