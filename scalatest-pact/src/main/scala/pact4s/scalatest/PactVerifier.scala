/*
 * Copyright 2021-2021 io.github.jbwheatley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pact4s.scalatest

import au.com.dius.pact.provider.{IConsumerInfo, VerificationResult}
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.PactVerifyResources

import scala.jdk.CollectionConverters._

trait PactVerifier extends AnyFlatSpec with PactVerifyResources {
  override private[pact4s] def verifySingleConsumer(consumer: IConsumerInfo): Unit =
    it should s"verify consumer '${consumer.getName}'" in {
      verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), providerInfo, consumer) match {
        case failed: VerificationResult.Failed =>
          verifier.displayFailures(List(failed).asJava)
          fail(s"Verification failed due to: ${failed.getDescription}")
        case _: VerificationResult.Ok => ()
        case _                        => throw new Exception("Impossible match failure")
      }
    }
}
