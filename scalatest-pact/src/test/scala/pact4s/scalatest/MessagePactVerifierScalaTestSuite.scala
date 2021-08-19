//package pact4s.scalatest
//
//import org.scalatest.BeforeAndAfterAll
//import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings
//import org.scalatest.flatspec.AnyFlatSpec
//import pact4s.{MockProviderServer, ProviderInfoBuilder}
//
//class MessagePactVerifierScalaTestSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll {
//  lazy val mock = new MockProviderServer(3458)
//
//  def provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
//    consumerName = "Pact4sMessageConsumer",
//    providerName = "Pact4sMessageProvider",
//    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json",
//    verificationSettings = Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.messages")))
//  )
//
//  it should "Verify pacts for provider `MessageProvider`" in {
//    verifyPacts()
//  }
//}
