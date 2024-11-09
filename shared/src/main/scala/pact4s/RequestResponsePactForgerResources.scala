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

import au.com.dius.pact.consumer.{BaseMockServer, MockHttpServerKt, PactVerificationResult}
import au.com.dius.pact.core.model.{RequestResponseInteraction, RequestResponsePact}
import pact4s.syntax.RequestResponsePactOps

import scala.jdk.CollectionConverters._

trait RequestResponsePactForgerResources
    extends BasePactForgerResources[RequestResponsePact]
    with RequestResponsePactOps {

  def interactions: List[RequestResponseInteraction] =
    // This seems to be the only reliable way to access RequestResponseInteraction across JDK versions
    pact.getInteractions.asScala.toList.collect { case interaction: RequestResponseInteraction => interaction }

  private[pact4s] def createServer: BaseMockServer = MockHttpServerKt.mockServer(pact, mockProviderConfig)

  private[pact4s] def verifyResultAndWritePactFiles(server: BaseMockServer): Either[Throwable, Unit] = {
    val result: PactVerificationResult =
      server.verifyResultAndWritePact(null, pactTestExecutionContext, pact, mockProviderConfig.getPactVersion)
    result match {
      case _: PactVerificationResult.Ok    => Right(())
      case e: PactVerificationResult.Error => Left(e.getError)
      case other                           => Left(new Error(s"Execution failed due to:\n ${other.getDescription}"))
    }
  }

}
