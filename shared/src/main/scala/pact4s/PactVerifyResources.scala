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
import pact4s.provider.StateManagement.StateManagementFunction
import pact4s.provider._
import sourcecode.{File, FileName, Line}

import scala.collection.mutable.ListBuffer
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

trait PactVerifyResources {
  type ResponseFactory = String => ResponseBuilder

  def provider: ProviderInfoBuilder

  def responseFactory: Option[ResponseFactory] = None

  private val failures: ListBuffer[String]        = new ListBuffer[String]()
  private val pendingFailures: ListBuffer[String] = new ListBuffer[String]()

  private[pact4s] def skip(message: String)(implicit fileName: FileName, file: File, line: Line): Unit
  private[pact4s] def failure(message: String)(implicit fileName: FileName, file: File, line: Line): Nothing

  private[pact4s] def runWithTimeout(
      verify: () => VerificationResult,
      timeout: Option[FiniteDuration]
  ): Either[Throwable, VerificationResult] = timeout match {
    case Some(timeout) =>
      try
        Right(
          TimeLimiter.callWithTimeout(verify, timeout)
        )
      catch {
        case NonFatal(e) => Left(e)
      }
    case None => Right(verify())
  }

  private def verifySingleConsumer(
      providerInfo: ProviderInfo,
      verifier: ProviderVerifier,
      timeout: Option[FiniteDuration]
  )(consumer: IConsumerInfo): Unit =
    runWithTimeout(() => runVerification(verifier, providerInfo, consumer), timeout) match {
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
      case Left(_: TimeoutException) =>
        failures += s"Verification of pact between ${providerInfo.getName} and ${consumer.getName} exceeded the time out of ${timeout.orNull}"
      case Left(e) =>
        failures += s"Verification of pact between ${providerInfo.getName} and ${consumer.getName} failed due to: ${e.getMessage}"
    }

  private[pact4s] def runVerification(
      verifier: ProviderVerifier,
      providerInfo: ProviderInfo,
      consumer: IConsumerInfo
  ): VerificationResult =
    verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), providerInfo, consumer, null)

  private def resolveProperty(properties: Map[String, String], name: String): Option[String] =
    properties.get(name).orElse(Option(System.getProperty(name)))

  private def setupVerifier(
      providerBranch: Option[Branch],
      publishVerificationResults: Option[PublishVerificationResults],
      providerMethodInstance: Option[AnyRef],
      providerVerificationOptions: List[ProviderVerificationOption]
  ): ProviderVerifier = {
    val verifier = new ProviderVerifier()
    val properties =
      publishVerificationResults
        .fold(providerVerificationOptions)(_ =>
          ProviderVerificationOption.VERIFIER_PUBLISH_RESULTS :: providerVerificationOptions
        )
        .map(opt => (opt.key, opt.value))
        .toMap
    verifier.setProviderBranch(() => providerBranch.map(_.branch).getOrElse(""))

    providerMethodInstance.foreach(instance => verifier.setProviderMethodInstance(_ => instance))
    verifier.setProjectGetProperty(p => resolveProperty(properties, p).orNull)
    verifier.setProjectHasProperty(name => resolveProperty(properties, name).isDefined)
    verifier.setProviderVersion(() => publishVerificationResults.map(_.providerVersion).getOrElse(""))
    verifier.setProviderTags(() =>
      publishVerificationResults.flatMap(_.providerTags.map(_.toList)).getOrElse(Nil).asJava
    )
    responseFactory.foreach { responseFactory =>
      verifier.setResponseFactory(description => responseFactory(description).build)
    }
    verifier
  }

  private def runWithStateChanger(run: => Unit): Unit = {
    val stateChanger: StateChanger =
      provider.stateManagement match {
        case Some(s: StateManagementFunction) =>
          new StateChanger.SimpleServer(s.stateChangeFunc, s.stateChangeBeforeHook, s.stateChangeAfterHook, s.host, s.port, s.endpoint)
        case _ => StateChanger.NoOpStateChanger
      }
    stateChanger.start()
    try run
    finally stateChanger.shutdown()
  }

  /** @param providerBranch
    *   the branch of the provider project from which verification is being run. Applicable if using the
    *   `MatchingBranch` selector, or can be used to label the verification results if they are being published.
    * @param publishVerificationResults
    *   if set, results of verification will be published to the pact broker, along with version and tags
    * @param providerMethodInstance
    *   The method instance to use when invoking methods with
    *   [[pact4s.provider.VerificationSettings.AnnotatedMethodVerificationSettings]].
    * @param providerVerificationOptions
    *   list of options to pass to the pact-jvm verifier
    */
  def verifyPacts(
      providerBranch: Option[Branch] = None,
      publishVerificationResults: Option[PublishVerificationResults] = None,
      providerMethodInstance: Option[AnyRef] = None,
      providerVerificationOptions: List[ProviderVerificationOption] = Nil,
      verificationTimeout: Option[FiniteDuration] = Some(30.seconds)
  )(implicit fileName: FileName, file: File, line: Line): Unit = {
    runWithStateChanger {
      val branch = providerBranch.orElse(publishVerificationResults.flatMap(_.providerBranch))
      val verifier =
        setupVerifier(branch, publishVerificationResults, providerMethodInstance, providerVerificationOptions)
      // to support deprecated branch settings using PublishVerificationResults
      val providerInfo =
        provider.build(branch, responseFactory) match {
          case Left(value) =>
            failure(s"${value.getMessage} - cause: ${Option(value.getCause).map(_.getMessage).orNull}")
          case Right(value) => value
        }

      verifier.initialiseReporters(providerInfo)

      providerInfo.getConsumers.asScala.foreach(verifySingleConsumer(providerInfo, verifier, verificationTimeout))
    }

    val failedMessages  = failures.toList
    val pendingMessages = pendingFailures.toList
    if (failedMessages.nonEmpty) failure(failedMessages.mkString("\n"))
    if (pendingMessages.nonEmpty) skip(pendingMessages.mkString("\n"))
  }
}
