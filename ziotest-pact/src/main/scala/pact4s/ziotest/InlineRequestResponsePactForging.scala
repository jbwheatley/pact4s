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

import au.com.dius.pact.consumer.{BaseMockServer, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import pact4s.{InlineRequestResponsePactResources, RequestResponsePactForgerResources}
import zio._
import zio.test.{TestResult, ZIOSpecDefault}

trait InlineRequestResponsePactForging extends ZIOSpecDefault with InlineRequestResponsePactResources { self =>
  private def serverResource(
      self: RequestResponsePactForgerResources
  ): RIO[Scope, BaseMockServer] = {
    import self._
    val server = createServer
    ZIO.acquireRelease {
      for {
        _ <- ZIO.fromEither(validatePactVersion(mockProviderConfig.getPactVersion))
        _ <- ZIO.attempt(server.start())
        _ <- ZIO.attempt(server.waitForServer())
      } yield server
    } { s =>
      ZIO.attempt(s.stop()).catchAll(e => ZIO.logError(s"failed to stop mock server: $e")).as(())
    }
  }

  sealed abstract class ForgerImpl extends InlineRequestResponsePactForger {
    def apply[S](test: BaseMockServer => ZIO[S, Throwable, TestResult]): ZIO[S, Throwable, TestResult]
  }

  override private[pact4s] type Forger = ForgerImpl

  override def withPact(aPact: RequestResponsePact): Forger =
    new ForgerImpl {
      override val pact: RequestResponsePact                          = aPact
      override val pactTestExecutionContext: PactTestExecutionContext = self.pactTestExecutionContext

      override def apply[S](test: BaseMockServer => ZIO[S, Throwable, TestResult]): ZIO[S, Throwable, TestResult] =
        ZIO.scoped {
          serverResource(this).flatMap { server =>
            for {
              res <- test(server)
              _   <- self.beforeWritePacts()
              _   <- ZIO.fromEither(verifyResultAndWritePactFiles(server))
            } yield res
          }
        }
    }

  override private[pact4s] type Effect[A] = UIO[A]

  def beforeWritePacts(): UIO[Unit] = ZIO.unit
}
