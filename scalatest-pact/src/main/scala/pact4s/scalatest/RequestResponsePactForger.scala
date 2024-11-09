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

package pact4s.scalatest

import au.com.dius.pact.consumer.BaseMockServer
import org.scalatest._
import pact4s.Pact4sLogger.{notWritingPactMessage, pact4sLogger}
import pact4s.RequestResponsePactForgerResources

trait RequestResponsePactForger extends RequestResponsePactForgerResources with SuiteMixin { self: Suite =>

  def mockServer: BaseMockServer = allocatedServer.get

  private var testFailed                              = false
  private var allocatedServer: Option[BaseMockServer] = None

  abstract override def run(testName: Option[String], args: Args): Status = {
    val server = createServer
    allocatedServer = Some(server)
    if (expectedTestCount(args.filter) == 0) {
      new CompositeStatus(Set.empty)
    } else {
      validatePactVersion(mockProviderConfig.getPactVersion).left.foreach(throw _)
      server.start()
      server.waitForServer()
      try {
        val result = super.run(testName, args)
        if (!result.succeeds())
          testFailed = true
        result
      } catch {
        case e: Throwable =>
          server.stop()
          throw e
      } finally {
        server.stop()
        if (testFailed) {
          pact4sLogger.error(
            notWritingPactMessage(pact)
          )
        } else {
          beforeWritePacts().flatMap { _ =>
            verifyResultAndWritePactFiles(server)
          } match {
            case Left(e)  => throw e
            case Right(_) => ()
          }
        }
      }
    }
  }

  override private[pact4s] type Effect[_] = Either[Throwable, _]

  def beforeWritePacts(): Either[Throwable, Unit] = Right(())
}
