//package pact4s.scalatest
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.flatspec.AnyFlatSpec
//import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
//import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
//
//class MessagePactVerifierBrokerScalaTestSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll {
//  lazy val mock = new MockProviderServer(3460)
//
//  def provider: ProviderInfoBuilder = mock.brokerProviderInfo(
//    providerName = "Pact4sMessageProvider",
//    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.messages")))
//  )
//
//  it should "Verify pacts for provider `MessageProvider`" in {
//    verifyPacts(
//      publishVerificationResults = Some(
//        PublishVerificationResults(
//          providerVersion = "SNAPSHOT",
//          providerTags = Nil
//        )
//      )
//    )
//  }
//}
