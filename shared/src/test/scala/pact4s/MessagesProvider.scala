package pact4s

import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
import io.circe.Json
import io.circe.syntax.EncoderOps

import scala.jdk.CollectionConverters._

// Provides message generators for annotated method verification.
// Because pact-jvm searches across the classpath, we can only define these once.
class MessagesProvider {

  @PactVerifyProvider("A message to say goodbye")
  def goodbyeMessage(): MessageAndMetadata = {
    val metadata = Map.empty[String, String]
    val body     = Json.obj("goodbye" -> "harry".asJson)
    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
  }

  @PactVerifyProvider("A message to say hello")
  def helloMessage(): MessageAndMetadata = {
    val metadata = Map("hi" -> "there")
    val body     = Json.obj("hello" -> "harry".asJson)
    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
  }
}
