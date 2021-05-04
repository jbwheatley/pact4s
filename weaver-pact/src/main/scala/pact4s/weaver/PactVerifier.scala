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

package pact4s.weaver

import au.com.dius.pact.provider.{IConsumerInfo, VerificationResult}
import pact4s.PactVerifyResources
import weaver.Expectations.Helpers.expect
import weaver.MutableFSuite

import scala.jdk.CollectionConverters._

trait PactVerifier[F[_]] extends MutableFSuite[F] with PactVerifyResources {
  override private[pact4s] def verifySingleConsumer(consumer: IConsumerInfo): Unit =
    pureTest(s"Verification of ${consumer.getName}") {
      val result =
        verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), providerInfo, consumer) match {
          case failed: VerificationResult.Failed =>
            verifier.displayFailures(List(failed).asJava)
            false
          case _: VerificationResult.Ok => true
        }
      expect(result)
    }
}
