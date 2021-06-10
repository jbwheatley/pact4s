package pact4s.messages

import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
import pact4s.MessageAndMetadataBuilder

// Provides message generators for annotated method verification.
// Because pact-jvm searches across the classpath, we can only define these once.
class MessagesProvider {

  @PactVerifyProvider("A message to say goodbye")
  def goodbyeMessage(): MessageAndMetadata = {
    val body = """{"goodbye":"harry"}"""
    MessageAndMetadataBuilder(body).build
  }

  @PactVerifyProvider("A message to say hello")
  def helloMessage(): MessageAndMetadata = {
    val metadata = Map("hi" -> "there")
    val body     = """{"hello":"harry"}"""
    MessageAndMetadataBuilder(body, metadata).build
  }
}
