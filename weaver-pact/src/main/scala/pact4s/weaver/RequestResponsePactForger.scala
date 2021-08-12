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
import cats.effect.{Ref, Resource}
import cats.implicits._
import cats.effect.implicits._
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

  private val testFailed: Ref[F, Boolean] = Ref.unsafe(false)

  private[weaver] def serverResource: Resource[F, BaseMockServer] = Resource.make[F, BaseMockServer] {
    val server = createServer
    for {
      _ <- validatePactVersion(mockProviderConfig.getPactVersion).liftTo[F]
      _ <- F.delay(server.start())
      _ <- F.delay(server.waitForServer())
    } yield server
  } { s =>
    testFailed.get
      .flatMap {
        case true =>
          pact4sLogger.info(
            s"Not writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to file because tests failed."
          )
          F.unit
        case false =>
          beforeWritePacts().as(
            pact4sLogger.info(
              s"Writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}"
            )
          ) >> F
            .delay(s.verifyResultAndWritePact(null, pactTestExecutionContext, pact, mockProviderConfig.getPactVersion))
            .void
      }
      .guarantee(F.delay(s.stop()))
  }

  override def spec(args: List[String]): fs2.Stream[F, TestOutcome] = super.spec(args).evalTap {
    case outcome if outcome.status.isFailed => testFailed.update(_ => true)
    case _                                  => F.unit
  }

  type Effect[_] = F[_]

  def beforeWritePacts(): F[Unit] = F.unit
}
