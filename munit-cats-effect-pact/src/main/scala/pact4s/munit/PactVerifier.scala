package pact4s.munit

import au.com.dius.pact.provider.{IConsumerInfo, VerificationResult}
import munit.CatsEffectSuite
import pact4s.PactVerifyResources

import scala.jdk.CollectionConverters._

trait PactVerifier extends CatsEffectSuite with PactVerifyResources {
  override private[pact4s] def verifySingleConsumer(consumer: IConsumerInfo): Unit =
    test(s"Verification of pact between ${providerInfo.getName} and ${consumer.getName}") {
      verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), providerInfo, consumer) match {
        case failed: VerificationResult.Failed =>
          verifier.displayFailures(List(failed).asJava)
          fail(s"Verification failed due to: ${failed.getDescription}")
        case _: VerificationResult.Ok => ()
      }
    }
}
