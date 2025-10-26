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

package pact4s.ziotest

import au.com.dius.pact.core.model.messaging.Message
import pact4s.MessagePactForgerResources
import pact4s.Pact4sLogger.{notWritingPactMessage, pact4sLogger}
import zio.test.{Spec, TestAspect, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, UIO, ZIO}

import scala.jdk.CollectionConverters._

trait MessagePactForger extends ZIOSpecDefault with MessagePactForgerResources {

  def specName: String = s"Pact: ${pact.getConsumer.getName} - ${pact.getProvider.getName}"

  def messages: List[Message] = pact.getMessages.asScala.toList

  private var testFailed = false

  def verify(message: Message): Spec[Any, Nothing] = message.getDescription match {
    case description => test(s"Missing verification for message: '$description'")(assertTrue(false))
  }

  def tests: Seq[Spec[Any, Nothing]] = messages.map(verify)

  override def spec: Spec[TestEnvironment with Scope, Throwable] = suite(specName)(tests) @@ TestAspect.aroundAllWith(
    for {
      validationPactVersion <- ZIO.attempt(validatePactVersion(pactSpecVersion))
      _                     <- ZIO.foreachDiscard(validationPactVersion.left.toSeq)(e =>
        ZIO.logError(s"failed to validate pact version: $e")
      )
    } yield validationPactVersion.isRight
  )(validatePactVersion =>
    if (validatePactVersion && !testFailed)
      beforeWritePacts() *> ZIO
        .attempt(writeMessagePactToFile())
        .catchAll(e => ZIO.logError(s"failed to write pact file: $e"))
    else
      ZIO.succeed(pact4sLogger.error(notWritingPactMessage(pact))).when(testFailed)
  ) @@ TestAspect.afterFailure(ZIO.succeed(this.testFailed = true))

  override private[pact4s] type Effect[A] = ZIO[Any, Throwable, A]

  override def beforeWritePacts(): UIO[Unit] = ZIO.unit
}
