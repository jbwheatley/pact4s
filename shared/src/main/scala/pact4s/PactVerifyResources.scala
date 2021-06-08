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

import au.com.dius.pact.provider.{IConsumerInfo, ProviderVerifier}

import scala.jdk.CollectionConverters._

trait PactVerifyResources {
  def provider: ProviderInfoBuilder
  val methodInstance: PactVerifyResources = this

  private[pact4s] def providerInfo = provider.toProviderInfo

  private[pact4s] val verifier = new ProviderVerifier()

  private[pact4s] def verifySingleConsumer(consumer: IConsumerInfo): Unit

  def verifyPacts(publishVerificationResults: Option[PublishVerificationResults] = None): Unit = {
    val properties: Map[String, String] = Map(
      ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS -> publishVerificationResults.isDefined.toString
    )

    val propertyResolver = new PactVerifierPropertyResolver(verifier, properties)

    verifier.initialiseReporters(providerInfo)
    verifier.setProviderMethodInstance(_ => methodInstance)
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

private[pact4s] final class PactVerifierPropertyResolver(verifier: ProviderVerifier, properties: Map[String, String]) {
  def getProperty(name: String): String =
    Option(verifier.getProjectGetProperty.apply(name)).getOrElse(properties.get(name).orNull)
}
