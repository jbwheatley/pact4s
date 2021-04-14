package pact4s.weaver

import au.com.dius.pact.provider.{IConsumerInfo, VerificationResult}
import pact4s.PactVerifyResources
import weaver.Expectations.Helpers.expect
import weaver.MutableFSuite

import scala.jdk.CollectionConverters._

trait PactVerifier[F[_]] extends MutableFSuite[F] with PactVerifyResources {
  override private[pact4s] def verifySingleConsumer(consumer: IConsumerInfo): Unit =
    pureTest(s"Verification of pact between ${provider.getName} and ${consumer.getName}") {
      val result =
        verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), provider, consumer) match {
          case failed: VerificationResult.Failed =>
            verifier.displayFailures(List(failed).asJava)
            false
          case _: VerificationResult.Ok => true
        }
      expect(result)
    }
}
