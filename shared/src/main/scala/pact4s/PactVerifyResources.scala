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
import pact4s.effect.MonadLike
import pact4s.effect.MonadLike._
import pact4s.provider.StateManagement.{ProviderUrl, StateManagementFunction}
import pact4s.provider._
import sourcecode.{File, FileName, Line}

import scala.collection.mutable.ListBuffer
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

trait PactVerifyResources[F[+_]] {
  type ResponseFactory = String => ResponseBuilder

  private[pact4s] implicit def F: MonadLike[F]

  def provider: ProviderInfoBuilder

  def responseFactory: Option[ResponseFactory] = None

  private val failures: ListBuffer[String]        = new ListBuffer[String]()
  private val pendingFailures: ListBuffer[String] = new ListBuffer[String]()

  private[pact4s] def skip(message: String)(implicit fileName: FileName, file: File, line: Line): F[Unit]
  private[pact4s] def failure(message: String)(implicit fileName: FileName, file: File, line: Line): F[Nothing]

  private[pact4s] def runWithTimeout(
      verify: => F[VerificationResult],
      timeout: Option[FiniteDuration]
  ): F[Either[Throwable, VerificationResult]] = timeout match {
    case Some(timeout) =>
      try
        TimeLimiter.callWithTimeout[F[VerificationResult]](verify, timeout).map(Right(_))
      catch {
        case NonFatal(e) => F(Left(e))
      }
    case None => verify.map(Right(_))
  }

  private def verifySingleConsumer(
      providerInfo: ProviderInfo,
      verifier: ProviderVerifier,
      timeout: Option[FiniteDuration]
  )(consumer: IConsumerInfo): F[Unit] =
    runWithTimeout(runVerification(verifier, providerInfo, consumer), timeout).flatMap {
      case Right(failed: VerificationResult.Failed) =>
        F(verifier.displayFailures(List(failed).asJava))
        // Don't fail the build if the pact is pending.
        val pending = failed.getPending
        val message =
          s"Verification of pact between ${providerInfo.getName} and ${consumer.getName} failed${if (pending)
              " [PENDING]"
            else ""}: '${failed.getDescription}'"
        if (pending) F(pendingFailures += message)
        else F(failures += message)
      case Right(_: VerificationResult.Ok) => F(())
      case Right(_)                        => failure("unexpected result type")
      case Left(_: TimeoutException) =>
        F {
          failures += s"Verification of pact between ${providerInfo.getName} and ${consumer.getName} exceeded the time out of ${timeout.orNull}"
        }
      case Left(e) =>
        F {
          failures += s"Verification of pact between ${providerInfo.getName} and ${consumer.getName} failed due to: ${e.getMessage}"
        }
    }

  private[pact4s] def runVerification(
      verifier: ProviderVerifier,
      providerInfo: ProviderInfo,
      consumer: IConsumerInfo
  ): F[VerificationResult] =
    F {
      verifier.runVerificationForConsumer(new java.util.HashMap[String, Object](), providerInfo, consumer, null)
    }

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

  private def runWithStateChanger(run: Option[String] => F[Unit]): F[Unit] =
    provider.getStateManagement match {
      case Some(s: StateManagementFunction) =>
        val stateChanger =
          new StateChanger.SimpleServer(s.stateChangeFunc, s.stateChangeBeforeHook, s.host, s.port, s.endpoint)

        for {
          _ <- F(stateChanger.start())
          _ <- run(Some(s.url(stateChanger.boundPort)))
          _ <- F(stateChanger.shutdown())
        } yield ()
      case Some(p: ProviderUrl) => run(Some(p.url))
      case None                 => run(None)
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
  )(implicit fileName: FileName, file: File, line: Line): F[Unit] =
    runWithStateChanger { stateChangeUrl =>
      val verifier: ProviderVerifier =
        setupVerifier(providerBranch, publishVerificationResults, providerMethodInstance, providerVerificationOptions)
      for {
        // to support deprecated branch settings using PublishVerificationResults
        providerInfo <- provider.build(providerBranch, responseFactory, stateChangeUrl) match {
          case Left(value) =>
            failure(s"${value.getMessage} - cause: ${Option(value.getCause).map(_.getMessage).orNull}")
          case Right(value) => F(value)
        }
        _ <- F(verifier.initialiseReporters(providerInfo))
        consumers = providerInfo.getConsumers.asScala.filter(verifier.filterConsumers)
        _ <-
          if (consumers.isEmpty) {
            F(verifier.getReporters.forEach(_.warnProviderHasNoConsumers(providerInfo)))
          } else F(())
        _ <- F.foreach(consumers.toList)(verifySingleConsumer(providerInfo, verifier, verificationTimeout))
        failedMessages  = failures.toList
        pendingMessages = pendingFailures.toList
        _ <-
          if (failedMessages.nonEmpty) failure(failedMessages.mkString("\n"))
          else if (pendingMessages.nonEmpty) skip(pendingMessages.mkString("\n"))
          else F(())
      } yield ()
    }
}
