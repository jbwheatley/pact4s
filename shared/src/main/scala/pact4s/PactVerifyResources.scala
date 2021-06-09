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

package pact4s

import au.com.dius.pact.provider.{IConsumerInfo, ProviderVerifier, VerificationResult}
import sourcecode.{File, FileName, Line}

import scala.jdk.CollectionConverters._

import scala.jdk.CollectionConverters._

trait PactVerifyResources {
  def provider: ProviderInfoBuilder

  private[pact4s] def providerInfo = provider.toProviderInfo

  private[pact4s] val verifier = new ProviderVerifier()

  private[pact4s] def failure(message: String)(implicit fileName: FileName, file: File, line: Line): Nothing

  private[pact4s] def verifySingleConsumer(
      consumer: IConsumerInfo
  )(implicit fileName: FileName, file: File, line: Line): Unit =
    verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), providerInfo, consumer) match {
      case failed: VerificationResult.Failed =>
        verifier.displayFailures(List(failed).asJava)
        failure(s"Verification failed:\n ${failed.toString}")
      case _: VerificationResult.Ok => ()
      case _                        => ???
    }

  def verifyPacts(
      publishVerificationResults: Option[PublishVerificationResults] = None,
      providerMethodInstance: Option[AnyRef] = None,
      showStacktrace: Boolean = true
  )(implicit fileName: FileName, file: File, line: Line): Unit = {
    val propertyResolver = new PactVerifierPropertyResolver(
      Map(
        ProviderVerifier.PACT_SHOW_STACKTRACE          -> showStacktrace.toString,
        ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS -> publishVerificationResults.isDefined.toString
      )
    )
    verifier.initialiseReporters(providerInfo)
    providerMethodInstance.foreach(instance => verifier.setProviderMethodInstance(_ => instance))
    verifier.setProjectGetProperty(propertyResolver.getProperty)
    verifier.setProjectHasProperty(name => Option(propertyResolver.getProperty(name)).isDefined)
    verifier.setProviderVersion(() => publishVerificationResults.map(_.providerVersion).getOrElse(""))
    verifier.setProviderTags(() => publishVerificationResults.map(_.providerTags).getOrElse(Nil).asJava)

    providerInfo.getConsumers.forEach(verifySingleConsumer(_))
  }
}

final case class PublishVerificationResults(
    providerVersion: String,
    providerTags: List[String]
)

private[pact4s] final class PactVerifierPropertyResolver(properties: Map[String, String]) {
  def getProperty(name: String): String = Option(System.getProperty(name)).getOrElse(properties.get(name).orNull)
}
