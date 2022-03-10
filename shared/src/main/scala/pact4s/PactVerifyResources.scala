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

import au.com.dius.pact.provider.{IConsumerInfo, PactVerification, ProviderVerifier, VerificationResult}
import pact4s.provider.StateManagement.StateManagementFunction
import pact4s.provider.{ProviderInfoBuilder, ProviderVerificationOption, PublishVerificationResults, ResponseBuilder}
import sourcecode.{File, FileName, Line}

import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

trait PactVerifyResources {
  type ResponseFactory = String => ResponseBuilder

  def provider: ProviderInfoBuilder

  private lazy val stateChanger: StateChanger =
    provider.stateManagement match {
      case Some(s: StateManagementFunction) =>
        new StateChanger.SimpleServer(s.stateChangeFunc, s.host, s.port, s.endpoint)
      case _ => StateChanger.NoOpStateChanger
    }

  def responseFactory: Option[ResponseFactory] = None

  private val failures: ListBuffer[String]        = new ListBuffer[String]()
  private val pendingFailures: ListBuffer[String] = new ListBuffer[String]()

  private[pact4s] lazy val providerInfo = provider.build

  private[pact4s] val verifier = new ProviderVerifier()

  private[pact4s] def skip(message: String)(implicit fileName: FileName, file: File, line: Line): Unit
  private[pact4s] def failure(message: String)(implicit fileName: FileName, file: File, line: Line): Nothing

  private[pact4s] def runWithTimeout(
      verify: () => VerificationResult,
      timeout: Option[FiniteDuration]
  ): Either[TimeoutException, VerificationResult] = timeout match {
    case Some(timeout) =>
      try
        Right(
          TimeLimiter.callWithTimeout(verify, timeout.toSeconds, TimeUnit.SECONDS)
        )
      catch {
        case e: TimeoutException => Left(e)
      }
    case None => Right(verify())
  }

  private def verifySingleConsumer(
      consumer: IConsumerInfo,
      timeout: Option[FiniteDuration]
  ): Unit =
    runWithTimeout(() => runVerification(consumer), timeout) match {
      case Right(failed: VerificationResult.Failed) =>
        verifier.displayFailures(List(failed).asJava)
        // Don't fail the build if the pact is pending.
        val pending = failed.getPending
        val message =
          s"Verification of pact between ${providerInfo.getName} and ${consumer.getName} failed${if (pending)
              " [PENDING]"
            else ""}: '${failed.getDescription}'"
        if (pending) pendingFailures += message else failures += message
      case Right(_: VerificationResult.Ok) => ()
      case Right(_)                        => ???
      case Left(_) =>
        failures += s"Verification of pact between ${providerInfo.getName} and ${consumer.getName} exceeded the time out of ${timeout.orNull}"
    }

  private[pact4s] def runVerification(consumer: IConsumerInfo): VerificationResult =
    verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), providerInfo, consumer, null)

  private def resolveProperty(properties: Map[String, String], name: String): Option[String] =
    properties.get(name).orElse(Option(System.getProperty(name)))

  /** @param publishVerificationResults
    *   if set, results of verification will be published to the pact broker, along with version and tags
    * @param providerMethodInstance
    *   The method instance to use when invoking methods with
    *   [[pact4s.provider.VerificationSettings.AnnotatedMethodVerificationSettings]].
    * @param providerVerificationOptions
    *   list of options to pass to the pact-jvm verifier
    */
  def verifyPacts(
      publishVerificationResults: Option[PublishVerificationResults] = None,
      providerMethodInstance: Option[AnyRef] = None,
      providerVerificationOptions: List[ProviderVerificationOption] = Nil,
      verificationTimeout: Option[FiniteDuration] = Some(30.seconds)
  )(implicit fileName: FileName, file: File, line: Line): Unit = {
    stateChanger.start()
    val properties =
      publishVerificationResults
        .fold(providerVerificationOptions)(_ =>
          ProviderVerificationOption.VERIFIER_PUBLISH_RESULTS :: providerVerificationOptions
        )
        .map(opt => (opt.key, opt.value))
        .toMap
    responseFactory.foreach { responseFactory =>
      providerInfo.setVerificationType(PactVerification.RESPONSE_FACTORY)
      verifier.setResponseFactory(description => responseFactory(description).build)
    }
    verifier.initialiseReporters(providerInfo)
    providerMethodInstance.foreach(instance => verifier.setProviderMethodInstance(_ => instance))
    verifier.setProjectGetProperty(p => resolveProperty(properties, p).orNull)
    verifier.setProjectHasProperty(name => resolveProperty(properties, name).isDefined)
    verifier.setProviderVersion(() => publishVerificationResults.map(_.providerVersion).getOrElse(""))
    verifier.setProviderTags(() =>
      publishVerificationResults.flatMap(_.providerTags.map(_.toList)).getOrElse(Nil).asJava
    )
    verifier.setProviderBranch(() => publishVerificationResults.flatMap(_.providerBranch).map(_.branch).getOrElse(""))

    providerInfo.getConsumers.forEach(verifySingleConsumer(_, verificationTimeout))

    stateChanger.shutdown()

    val failedMessages  = failures.toList
    val pendingMessages = pendingFailures.toList
    if (failedMessages.nonEmpty) failure(failedMessages.mkString("\n"))
    if (pendingMessages.nonEmpty) skip(pendingMessages.mkString("\n"))
  }
}
