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

import au.com.dius.pact.consumer.{BaseMockServer, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.Resource
import cats.syntax.all._
import pact4s.Pact4sLogger.{notWritingPactMessage, pact4sLogger}
import pact4s.{InlineRequestResponsePactResources, RequestResponsePactForgerResources}
import weaver.{Expectations, MutableFSuite}

trait InlineRequestResponsePactForging[F[_]] extends MutableFSuite[F] with InlineRequestResponsePactResources { self =>
  private val F = effect
  private[weaver] def serverResource(self: RequestResponsePactForgerResources): Resource[F, BaseMockServer] = {
    import self._
    Resource.eval(F.delay(createServer)).flatMap { server =>
      Resource.make[F, BaseMockServer] {
        {
          for {
            _ <- validatePactVersion(mockProviderConfig.getPactVersion).liftTo[F]
            _ <- F.delay(server.start())
            _ <- F.delay(server.waitForServer())
          } yield server
        }.onError { case _: Throwable => F.delay(server.stop()) }
      } { s =>
        F.delay(s.stop())
      }
    }
  }

  sealed abstract class ForgerImpl extends InlineRequestResponsePactForger {
    def apply(test: BaseMockServer => F[Expectations]): F[Expectations]
  }

  override private[pact4s] type Forger = ForgerImpl

  override def withPact(aPact: RequestResponsePact): Forger =
    new ForgerImpl {
      override val pact: RequestResponsePact                          = aPact
      override val pactTestExecutionContext: PactTestExecutionContext = self.pactTestExecutionContext

      override def apply(test: BaseMockServer => F[Expectations]): F[Expectations] = serverResource(this).use { server =>
        {
          for {
            res <- test(server)
            _   <- self.beforeWritePacts()
            _   <- verifyResultAndWritePactFiles(server).liftTo[F]
          } yield res
        }.onError { case e: Throwable =>
          F.delay(
            pact4sLogger.error(e)(
              notWritingPactMessage(pact)
            )
          )
        }
      }
    }

  override private[pact4s] type Effect[A] = F[A]

  def beforeWritePacts(): F[Unit] = F.unit
}
