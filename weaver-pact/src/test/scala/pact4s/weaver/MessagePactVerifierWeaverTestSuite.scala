package pact4s.weaver

import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
import cats.effect.{IO, Resource}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.server.Server
import pact4s.{MockProviderServer, ProviderInfoBuilder, VerificationType}
import weaver.IOSuite

import scala.jdk.CollectionConverters._

object MessagePactVerifierWeaverTestSuite extends IOSuite with PactVerifier[IO] {
  type Res = Server

  val mock = new MockProviderServer(1236)

  override def sharedResource: Resource[IO, Server] = mock.server

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "MessageConsumer",
    providerName = "MessageProvider",
    fileName = "./scripts/MessageConsumer-MessageProvider.json",
    verificationType = VerificationType.AnnotatedMethod
  )

  verifyPacts()
}

// these methods must be in a class with no constructor arguments
class MessagePacts {
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
