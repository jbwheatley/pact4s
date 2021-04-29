package pact4s.scalatest

import au.com.dius.pact.provider.{IConsumerInfo, VerificationResult}
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.PactVerifyResources

import scala.jdk.CollectionConverters._

trait PactVerifier extends AnyFlatSpec with PactVerifyResources {
  override private[pact4s] def verifySingleConsumer(consumer: IConsumerInfo): Unit =
    it should s"Verification of pact between ${providerInfo.getName} and ${consumer.getName}" in {
      verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), providerInfo, consumer) match {
        case failed: VerificationResult.Failed =>
          verifier.displayFailures(List(failed).asJava)
          fail(s"Verification failed due to: ${failed.getDescription}")
        case _: VerificationResult.Ok => ()
      }
    }
}
