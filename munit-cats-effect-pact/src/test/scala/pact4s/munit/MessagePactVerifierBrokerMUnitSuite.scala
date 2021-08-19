//package pact4s.munit
//
//import munit.CatsEffectSuite
//import pact4s.{MockProviderServer, ProviderInfoBuilder, PublishVerificationResults}
//import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
//
//class MessagePactVerifierBrokerMUnitSuite extends CatsEffectSuite with PactVerifier {
//  val mock = new MockProviderServer(2348)
//
//  override val provider: ProviderInfoBuilder = mock.brokerProviderInfo(
//    providerName = "Pact4sMessageProvider",
//    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.messages")))
//  )
//
//  verifyPacts(
//    publishVerificationResults = Some(
//      PublishVerificationResults(
//        providerVersion = "SNAPSHOT",
//        providerTags = Nil
//      )
//    )
//  )
//}
