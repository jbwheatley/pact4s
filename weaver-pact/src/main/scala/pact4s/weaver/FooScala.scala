package foo
import au.com.dius.pact.provider.{IConsumerInfo, ProviderInfo, ProviderVerifier, VerificationResult}

import scala.jdk.CollectionConverters._

object Test {
  val verifier = new ProviderVerifier()
  def makeResult(provider: ProviderInfo, consumer: IConsumerInfo): Boolean =
    verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), provider, consumer) match {
      case failed: VerificationResult.Failed =>
        verifier.displayFailures(List(failed).asJava)
        false
      case _: VerificationResult.Ok => true
    }
}
