package pact4s.munit

import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
import io.circe.Json
import io.circe.syntax.EncoderOps
import pact4s.{MockProviderServer, ProviderInfoBuilder, VerificationType}

import scala.jdk.CollectionConverters._

class MessagePactVerifierMUnitSuite extends PactVerifier {
  val mock = new MockProviderServer(2347)

  override val provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "MessageConsumer",
    providerName = "MessageProvider",
    fileName = "./scripts/MessageConsumer-MessageProvider.json",
    verificationType = VerificationType.AnnotatedMethod
  )

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
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

  verifyPacts()
}
