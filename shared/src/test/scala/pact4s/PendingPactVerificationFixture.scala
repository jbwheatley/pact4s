package pact4s

import au.com.dius.pact.provider.{
  IConsumerInfo,
  ProviderInfo,
  ProviderVerifier,
  VerificationFailureType,
  VerificationResult
}
import org.mockito.MockitoSugar

import java.util
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

/** Provides a test fixture for PactVerifyResources which produces a consumer pact failure that is pending.
  * @see
  *   https://github.com/jbwheatley/pact4s/pull/52
  */
trait PendingPactVerificationFixture extends MockitoSugar { this: PactVerifyResources =>
  override private[pact4s] val verifier = mock[ProviderVerifier]
  override private[pact4s] lazy val providerInfo = {
    val providerInfo = mock[ProviderInfo]
    val consumer     = mock[IConsumerInfo]
    when(providerInfo.getConsumers).thenReturn(List(consumer).asJava)
    providerInfo
  }

  lazy val provider: ProviderInfoBuilder = ProviderInfoBuilder("", mock[PactSource])

  @nowarn("cat=unused")
  override private[pact4s] def runVerification(consumer: IConsumerInfo): VerificationResult = {
    val description: String             = "description"
    val verificationDescription: String = "verificationDescription"
    val failures: util.Map[String, util.List[VerificationFailureType]] =
      Map[String, util.List[VerificationFailureType]]().asJava
    val pending: Boolean                             = true
    val results: util.List[util.Map[String, AnyRef]] = List[util.Map[String, AnyRef]]().asJava
    new VerificationResult.Failed(description, verificationDescription, failures, pending, results)
  }
}
