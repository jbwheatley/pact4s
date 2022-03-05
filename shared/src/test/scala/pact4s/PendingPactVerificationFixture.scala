package pact4s

import au.com.dius.pact.provider._
import org.mockito.Mockito._
import pact4s.provider.ProviderInfoBuilder

import java.util
import scala.jdk.CollectionConverters._

/** Provides a test fixture for PactVerifyResources which produces a consumer pact failure that is pending.
  * @see
  *   https://github.com/jbwheatley/pact4s/pull/52
  */
trait PendingPactVerificationFixture { this: PactVerifyResources =>
  val mocks = new Mocks()
  override private[pact4s] val verifier: ProviderVerifier =
    mocks.providerVerifier
  override private[pact4s] lazy val providerInfo = {
    val providerInfo: ProviderInfo = mocks.providerInfo
    val consumer: IConsumerInfo    = mocks.consumerInfo
    when(providerInfo.getName).thenReturn("PendingPactProvider")
    when(consumer.getName).thenReturn("PendingPactConsumer")
    when(providerInfo.getConsumers).thenReturn(List(consumer).asJava)
    providerInfo
  }

  lazy val provider: ProviderInfoBuilder =
    ProviderInfoBuilder("", mocks.pactSource)

  override private[pact4s] def runVerification(consumer: IConsumerInfo): VerificationResult = {
    val _                               = consumer
    val description: String             = "description"
    val verificationDescription: String = "verificationDescription"
    val failures: util.Map[String, util.List[VerificationFailureType]] =
      Map[String, util.List[VerificationFailureType]]().asJava
    val pending: Boolean                             = true
    val results: util.List[util.Map[String, AnyRef]] = List[util.Map[String, AnyRef]]().asJava
    new VerificationResult.Failed(description, verificationDescription, failures, pending, results)
  }
}
