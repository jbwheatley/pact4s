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

package pact4s.scalatest.message

import au.com.dius.pact.core.model.{Interaction, Pact, PactSource, UrlPactSource}
import au.com.dius.pact.provider.reporters.{BaseVerifierReporter, VerifierReporter}
import au.com.dius.pact.provider.{IConsumerInfo, IProviderInfo, IProviderVerifier, VerificationResult}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.ProviderInfoBuilder
import pact4s.scalatest.MessagePactVerifier

import java.io.File
import java.net.ServerSocket
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._

/** Exercises the `additionalReporters` parameter of `verifyPacts`. The probe extends `BaseVerifierReporter` with no-op
  * overrides for every callback except `reportVerificationForConsumer`, which records the consumer name. If
  * `additionalReporters` is wired into `ProviderVerifier.setReporters` correctly, the recorded list must contain the
  * fixture consumer at least once.
  */
class AdditionalReportersFileSuite extends AnyFlatSpec with Matchers with MessagePactVerifier {
  lazy val mock = new MockProviderServer(AdditionalReportersFileSuite.freePort())

  def messages: ResponseFactory     = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.fileSourceMessageProviderInfo

  it should "register additional reporters and route per-consumer callbacks through them" in {
    val consumers = new ConcurrentLinkedQueue[String]()

    val probe: VerifierReporter = new NoOpVerifierReporter {
      override def reportVerificationForConsumer(
          consumer: IConsumerInfo,
          provider: IProviderInfo,
          tag: String
      ): Unit = {
        consumers.add(consumer.getName)
        ()
      }
    }

    verifyPacts(additionalReporters = List(probe))

    // Asserts wiring (probe was invoked), exactly-once-per-consumer routing,
    // and that the right IConsumerInfo flowed through (name pinned to fixture).
    consumers.asScala.toList shouldBe List("Pact4sMessageConsumer")
  }
}

object AdditionalReportersFileSuite {
  // Bind to port 0, read the OS-assigned port, release it. There's a small race between close()
  // and MockProviderServer rebinding, but it's good enough for a single-test fixture and avoids
  // collisions across parallel CI runs.
  private def freePort(): Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()
  }
}

/** No-op base for `VerifierReporter`-based test probes. `BaseVerifierReporter` only implements `receive`; everything
  * else on `VerifierReporter` is abstract and all concrete reporters in pact-jvm are `final`, so a no-op subclass is
  * the simplest way to override a single callback (here, `reportVerificationForConsumer`) without inheriting unwanted
  * side effects.
  */
private abstract class NoOpVerifierReporter extends BaseVerifierReporter {
  // Kotlin's `val ext: String?` and `var reportDir/reportFile/verifier` on the JVM are accessor
  // pairs (getExt, getReportDir/setReportDir, …). Override those rather than Scala `val`/`var`.
  private var _reportDir: File                             = null
  private var _reportFile: File                            = new File("")
  private var _verifier: IProviderVerifier                 = _
  override def getExt: String                              = null
  override def getReportDir: File                          = _reportDir
  override def setReportDir(value: File): Unit             = _reportDir = value
  override def getReportFile: File                         = _reportFile
  override def setReportFile(value: File): Unit            = _reportFile = value
  override def getVerifier: IProviderVerifier              = _verifier
  override def setVerifier(value: IProviderVerifier): Unit = _verifier = value

  override def initialise(provider: IProviderInfo): Unit                                                          = ()
  override def finaliseReport(): Unit                                                                             = ()
  override def reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, tag: String): Unit = ()
  override def verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo): Unit                       = ()
  override def verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo): Unit                        = ()
  override def pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String): Unit                         = ()
  override def warnProviderHasNoConsumers(provider: IProviderInfo): Unit                                          = ()
  override def warnPactFileHasNoInteractions(pact: Pact): Unit                                                    = ()
  override def interactionDescription(interaction: Interaction): Unit                                             = ()
  override def stateForInteraction(
      state: String,
      provider: IProviderInfo,
      consumer: IConsumerInfo,
      isSetup: Boolean
  ): Unit                                                                                                    = ()
  override def warnStateChangeIgnored(state: String, provider: IProviderInfo, consumer: IConsumerInfo): Unit = ()
  override def stateChangeRequestFailedWithException(
      state: String,
      isSetup: Boolean,
      e: Exception,
      printStackTrace: Boolean
  ): Unit = ()
  override def stateChangeRequestFailed(
      state: String,
      provider: IProviderInfo,
      isSetup: Boolean,
      httpStatus: String
  ): Unit = ()
  override def warnStateChangeIgnoredDueToInvalidUrl(
      state: String,
      provider: IProviderInfo,
      isSetup: Boolean,
      stateChangeHandler: Any
  ): Unit = ()
  override def requestFailed(
      provider: IProviderInfo,
      interaction: Interaction,
      interactionMessage: String,
      e: Exception,
      printStackTrace: Boolean
  ): Unit                                                                                                 = ()
  override def returnsAResponseWhich(): Unit                                                              = ()
  override def statusComparisonOk(status: Int): Unit                                                      = ()
  override def statusComparisonFailed(status: Int, comparison: Any): Unit                                 = ()
  override def includesHeaders(): Unit                                                                    = ()
  override def headerComparisonOk(key: String, value: java.util.List[String]): Unit                       = ()
  override def headerComparisonFailed(key: String, value: java.util.List[String], comparison: Any): Unit  = ()
  override def bodyComparisonOk(): Unit                                                                   = ()
  override def bodyComparisonFailed(comparison: Any): Unit                                                = ()
  override def errorHasNoAnnotatedMethodsFoundForInteraction(interaction: Interaction): Unit              = ()
  override def verificationFailed(interaction: Interaction, e: Exception, printStackTrace: Boolean): Unit = ()
  override def generatesAMessageWhich(): Unit                                                             = ()
  override def displayFailures(failures: java.util.List[VerificationResult.Failed]): Unit                 = ()
  override def includesMetadata(): Unit                                                                   = ()
  override def metadataComparisonOk(): Unit                                                               = ()
  override def metadataComparisonOk(key: String, value: Any): Unit                                        = ()
  override def metadataComparisonFailed(key: String, value: Any, comparison: Any): Unit                   = ()
}
