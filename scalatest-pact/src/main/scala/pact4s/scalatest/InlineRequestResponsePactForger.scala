/*
 * Copyright 2024 io.github.jbwheatley
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
package scalatest

import au.com.dius.pact.consumer.{BaseMockServer, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import pact4s.syntax.RequestResponsePactOps
import pact4s.{PactTestExecutionContextAware, RequestResponsePactForgerResources}

trait InlineRequestResponsePactForger extends RequestResponsePactForgerResources {

  def apply[A](test: BaseMockServer => A): A = {
    val server = createServer

    validatePactVersion(mockProviderConfig.getPactVersion).left.foreach(throw _)
    server.start()
    server.waitForServer()
    try {
      val result = test(server)
      verifyResultAndWritePactFiles(server).fold[Unit](t => throw t, _ => ())
      result
    } catch {
      case e: Throwable =>
        server.stop()
        pact4sLogger.error(
          notWritingPactMessage(pact)
        )
        throw e
    } finally server.stop()
  }

  override type Effect[_] = Either[Throwable, _]

  override def beforeWritePacts(): Either[Throwable, Unit] = Right(())
}

trait InlineRequestResponsePactForging extends RequestResponsePactOps with PactTestExecutionContextAware {
  private def executionContext = pactTestExecutionContext

  def withPact(aPact: RequestResponsePact): InlineRequestResponsePactForger = new InlineRequestResponsePactForger {
    override val pactTestExecutionContext: PactTestExecutionContext = executionContext
    override def pact: RequestResponsePact                          = aPact
  }
}
