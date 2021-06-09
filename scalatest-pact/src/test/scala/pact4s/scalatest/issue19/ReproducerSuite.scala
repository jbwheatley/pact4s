package pact4s.scalatest.issue19

import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.BeforeAndAfterAll
import pact4s.{MockProviderServer, ProviderInfoBuilder}
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
import pact4s.scalatest.PactVerifier

import scala.jdk.CollectionConverters._

class ReproducerSuite extends PactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer(3459)

  // Because this is mutable, it will trigger the issue.
  var metadata: Map[String, String] = _

  def provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sMessageConsumer",
    providerName = "Pact4sMessageProvider",
    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json",
    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.scalatest.issue19")))
  )

  @PactVerifyProvider("A message to say goodbye")
  def goodbyeMessage(): MessageAndMetadata = {
    val metadata = Map.empty[String, String]
    val body     = Json.obj("goodbye" -> "harry".asJson)
    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
  }

  @PactVerifyProvider("A message to say hello")
  def helloMessage(): MessageAndMetadata = {
    val body = Json.obj("hello" -> "harry".asJson)
    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
  }

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    metadata = Map("hi" -> "there")
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  verifyPacts(
    // Issue #19
    // If the declaring class of the annotated method has mutable properties, we must ensure the verifier uses the same
    // instance is used when invoking the method, otherwise the state of the property will be wrong.
    providerMethodInstance = Some(this)
  )
}
