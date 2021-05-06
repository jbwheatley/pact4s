/*
 * Copyright 2021-2021 io.github.jbwheatley
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

import au.com.dius.pact.core.model.messaging.Message
import cats.effect.{IO, Resource, SyncIO}
import munit.internal.PlatformCompat
import munit.{CatsEffectSuite, Location, TestOptions}
import pact4s.MessagePactForgerResources

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.control.NonFatal

trait MessagePactForger extends CatsEffectSuite with MessagePactForgerResources {

  @volatile private var testFailed = false

  val messages: SyncIO[FunFixture[List[Message]]] = ResourceFixture(
    Resource.pure[IO, List[Message]](pact.getMessages.asScala.toList)
  )

  override def test(options: TestOptions)(body: => Any)(implicit loc: Location): Unit =
    munitTestsBuffer += munitTestTransform(
      new Test(
        options.name,
        () =>
          try PlatformCompat.waitAtMost(munitValueTransform(body), munitTimeout)
          catch {
            case NonFatal(e) =>
              testFailed = true
              Future.failed(e)
          },
        options.tags,
        loc
      )
    )

  override def beforeAll(): Unit = {
    super.beforeAll()
    validatePactVersion(pactSpecVersion).left.foreach[Unit](throw _)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    if (testFailed) {
      logger.info(
        s"Not writing message pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to file because tests failed."
      )
    } else {
      logger.info(
        s"Writing message pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}"
      )
      val write = pact.write(pactTestExecutionContext.getPactFolder, pactSpecVersion)
      Option(write.component2()).foreach(throw _)
    }
  }
}
