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

package pact4s.weaver

import au.com.dius.pact.core.model.messaging.Message
import cats.effect.Resource
import cats.implicits._
import pact4s.MessagePactForgerResources
import weaver.{MutableFSuite, TestOutcome}

import scala.jdk.CollectionConverters._

trait SimpleMessagePactForger[F[_]] extends WeaverMessagePactForgerBase[F] {
  override type Res = List[Message]
  override def sharedResource: Resource[F, List[Message]] = messagesResource
}

trait MessagePactForger[F[_]] extends WeaverMessagePactForgerBase[F] {
  type Resources
  override type Res = (Resources, List[Message])

  def additionalSharedResource: Resource[F, Resources]

  override def sharedResource: Resource[F, (Resources, List[Message])] =
    (additionalSharedResource, messagesResource).tupled
}

trait WeaverMessagePactForgerBase[F[_]] extends MutableFSuite[F] with MessagePactForgerResources {
  private val F = effect

  @volatile private var testFailed: Boolean = false

  private[weaver] def messagesResource: Resource[F, List[Message]] = Resource.make[F, List[Message]] {
    validatePactVersion(pactSpecVersion).liftTo[F].as {
      pact.getMessages.asScala.toList
    }
  } { _ =>
    if (testFailed) {
      F.delay(
        pact4sLogger.error(
          notWritingPactMessage(pact)
        )
      )
    } else {
      beforeWritePacts() >> writeMessagePactToFile().liftTo[F]
    }
  }

  override def spec(args: List[String]): fs2.Stream[F, TestOutcome] = super.spec(args).evalTap {
    case outcome if outcome.status.isFailed => F.delay { testFailed = true }
    case _                                  => F.unit
  }

  type Effect[A] = F[A]

  def beforeWritePacts(): F[Unit] = F.unit
}
