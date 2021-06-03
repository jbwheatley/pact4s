package pact4s.scalatest

import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.BeforeAndAfterAll
import pact4s.{MockProviderServer, ProviderInfoBuilder, VerificationType}

import scala.jdk.CollectionConverters._

class MessagePactVerifierScalaTestSuite extends PactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer(3458)
  def provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    "MessageConsumer",
    "MessageProvider",
    "./scripts/MessageConsumer-MessageProvider.json",
    VerificationType.AnnotatedMethod
  )

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

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  verifyPacts()
}
