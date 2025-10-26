/*
 * Copyright 2021 io.github.jbwheatley
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
trait PendingPactVerificationFixture[F[+_]] { this: PactVerifyResources[F] =>
  val mocks        = new Mocks()
  val providerInfo = {
    val providerInfo: ProviderInfo = mocks.providerInfo
    val consumer: IConsumerInfo    = mocks.consumerInfo
    when(providerInfo.getName).thenReturn("PendingPactProvider")
    when(consumer.getName).thenReturn("PendingPactConsumer")
    when(providerInfo.getConsumers).thenReturn(List(consumer).asJava)
    providerInfo
  }

  lazy val provider: ProviderInfoBuilder = {
    val builder: ProviderInfoBuilder = mocks.providerInfoBuilder
    when(builder.build(None, None, None)).thenReturn(Right(providerInfo))
    when(builder.getStateManagement).thenReturn(None)
    builder
  }

  override private[pact4s] def runVerification(
      verifier: ProviderVerifier,
      providerInfo: ProviderInfo,
      consumer: IConsumerInfo
  ): F[VerificationResult] = {
    val _                                                              = (consumer, providerInfo, verifier)
    val description: String                                            = "description"
    val verificationDescription: String                                = "verificationDescription"
    val failures: util.Map[String, util.List[VerificationFailureType]] =
      Map[String, util.List[VerificationFailureType]]().asJava
    val pending: Boolean                             = true
    val results: util.List[util.Map[String, AnyRef]] = List[util.Map[String, AnyRef]]().asJava
    F(new VerificationResult.Failed(description, verificationDescription, failures, pending, results))
  }
}
