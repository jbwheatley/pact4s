//package pact4s.scalatest.issue19
//
//import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
//import io.circe.Json
//import io.circe.syntax.EncoderOps
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.flatspec.AnyFlatSpec
//import pact4s.{MessageAndMetadataBuilder, MockProviderServer, ProviderInfoBuilder}
//import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
//import pact4s.scalatest.PactVerifier
//
//import scala.jdk.CollectionConverters._
//
//class ReproducerSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll {
//  lazy val mock = new MockProviderServer(3459)
//
//  // Because this is mutable, it will trigger the issue.
//  var metadata: Map[String, String] = _
//
//  def provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
//    consumerName = "Pact4sMessageConsumer",
//    providerName = "Pact4sMessageProvider",
//    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json",
//    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.scalatest.issue19")))
//  )
//
//  @PactVerifyProvider("A message to say goodbye")
//  def goodbyeMessage(): MessageAndMetadata = {
//    val metadata = Map.empty[String, String]
//    val body     = Json.obj("goodbye" -> "harry".asJson)
//    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
//  }
//
//  @PactVerifyProvider("A message to say hello")
//  def helloMessage(): MessageAndMetadata = {
//    val body = Json.obj("hello" -> "harry".asJson)
//    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
//  }
//
//  @PactVerifyProvider("A message with nested arrays in the body")
//  def nestedArrayMessage(): MessageAndMetadata = {
//    val body = """{"array": [1,2,3]}"""
//    MessageAndMetadataBuilder(body).build
//  }
//
//  @PactVerifyProvider("A message with a json array as content")
//  def topLevelArrayMessage(): MessageAndMetadata = {
//    val body = """[{"a":1},{"b":true}]"""
//    MessageAndMetadataBuilder(body).build
//  }
//
//  override def beforeAll(): Unit =
//    metadata = Map("hi" -> "there")
//
//  it should "verify pacts" in {
//    verifyPacts(
//      // Issue #19
//      // If the declaring class of the annotated method has mutable properties, we must ensure the verifier uses the same
//      // instance is used when invoking the method, otherwise the state of the property will be wrong.
//      providerMethodInstance = Some(this)
//    )
//  }
//}
