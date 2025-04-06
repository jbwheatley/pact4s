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

package pact4s.munit

import au.com.dius.pact.consumer.{BaseMockServer, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.{IO, Resource}
import cats.syntax.all._
import munit.CatsEffectSuite
import pact4s.Pact4sLogger.{notWritingPactMessage, pact4sLogger}
import pact4s.{InlineRequestResponsePactResources, RequestResponsePactForgerResources}

trait InlineRequestResponsePactForging extends CatsEffectSuite with InlineRequestResponsePactResources { self =>
  private def serverResource(self: RequestResponsePactForgerResources): Resource[IO, BaseMockServer] = {
    import self._
    val server = createServer
    Resource.make[IO, BaseMockServer] {
      {
        for {
          _ <- validatePactVersion(mockProviderConfig.getPactVersion).liftTo[IO]
          _ <- IO(server.start())
          _ <- IO(server.waitForServer())
        } yield server
      }.onError { case _: Throwable => IO(server.stop()) }
    } { s =>
      IO(s.stop())
    }
  }

  sealed abstract class ForgerImpl extends InlineRequestResponsePactForger {
    def apply(test: BaseMockServer => IO[Unit]): IO[Unit]
  }

  override private[pact4s] type Forger = ForgerImpl

  override def withPact(aPact: RequestResponsePact): Forger =
    new ForgerImpl {
      override val pact: RequestResponsePact                          = aPact
      override val pactTestExecutionContext: PactTestExecutionContext = self.pactTestExecutionContext

      override def apply(test: BaseMockServer => IO[Unit]): IO[Unit] = serverResource(this).use { server =>
        {
          for {
            _ <- test(server)
            _ <- self.beforeWritePacts()
            _ <- verifyResultAndWritePactFiles(server).liftTo[IO]
          } yield ()
        }.onError { case e: Throwable =>
          IO(
            pact4sLogger.error(e)(
              notWritingPactMessage(pact)
            )
          )
        }
      }
    }

  override private[pact4s] type Effect[A] = IO[A]

  def beforeWritePacts(): IO[Unit] = IO.unit
}
