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

import au.com.dius.pact.consumer.BaseMockServer
import cats.effect.Resource
import cats.implicits._
import pact4s.RequestResponsePactForgerResources
import weaver.{MutableFSuite, TestOutcome}

trait SimpleRequestResponsePactForger[F[_]] extends WeaverRequestResponsePactForgerBase[F] {
  override type Res = BaseMockServer
  override def sharedResource: Resource[F, BaseMockServer] = serverResource
}

trait RequestResponsePactForger[F[_]] extends WeaverRequestResponsePactForgerBase[F] {

  type Resources
  override type Res = (Resources, BaseMockServer)

  def additionalSharedResource: Resource[F, Resources]

  override def sharedResource: Resource[F, (Resources, BaseMockServer)] =
    (additionalSharedResource, serverResource).tupled
}

trait WeaverRequestResponsePactForgerBase[F[_]] extends MutableFSuite[F] with RequestResponsePactForgerResources {
  private val F = effect

  @volatile private var testFailed: Boolean = false

  private[weaver] def serverResource: Resource[F, BaseMockServer] = {
    val server = createServer
    Resource.make[F, BaseMockServer] {
      {
        for {
          _ <- validatePactVersion(mockProviderConfig.getPactVersion).liftTo[F]
          _ <- F.delay(server.start())
          _ <- F.delay(server.waitForServer())
        } yield server
      }.onError { case _ => F.delay(server.stop()) }
    } { s =>
      F.delay(s.stop()) >> {
        if (testFailed) {
          F.delay(
            pact4sLogger.error(
              notWritingPactMessage(pact)
            )
          )
        } else {
          beforeWritePacts() >> verifyResultAndWritePactFiles(s).liftTo[F]
        }
      }
    }
  }

  override def spec(args: List[String]): fs2.Stream[F, TestOutcome] = super.spec(args).evalTap {
    case outcome if outcome.status.isFailed => F.delay { testFailed = true }
    case _                                  => F.unit
  }

  type Effect[A] = F[A]

  def beforeWritePacts(): F[Unit] = F.unit
}
