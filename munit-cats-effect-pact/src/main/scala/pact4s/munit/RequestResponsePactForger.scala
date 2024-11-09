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

import au.com.dius.pact.consumer.BaseMockServer
import cats.effect.{IO, Resource}
import cats.syntax.all._
import munit.catseffect.IOFixture
import munit.internal.PlatformCompat
import munit.{AnyFixture, CatsEffectSuite, Location, TestOptions}
import pact4s.Pact4sLogger.{notWritingPactMessage, pact4sLogger}
import pact4s.RequestResponsePactForgerResources

import scala.concurrent.Future
import scala.util.control.NonFatal

trait RequestResponsePactForger extends CatsEffectSuite with RequestResponsePactForgerResources {

  private var testFailed: Boolean = false

  override def munitFixtures: Seq[AnyFixture[_]] = serverFixture +: additionalMunitFixtures

  def additionalMunitFixtures: Seq[AnyFixture[_]] = Seq.empty

  private lazy val serverFixture: IOFixture[BaseMockServer] = ResourceSuiteLocalFixture(
    "mockHttpServer",
    serverResource
  )

  private def serverResource: Resource[IO, BaseMockServer] = {
    val server = createServer
    Resource.make[IO, BaseMockServer] {
      {
        for {
          _ <- validatePactVersion(mockProviderConfig.getPactVersion).liftTo[IO]
          _ <- IO(server.start())
          _ <- IO(server.waitForServer())
        } yield server
      }.onError(_ => IO(server.stop()))
    } { s =>
      IO(s.stop()) >> {
        if (testFailed) {
          IO(
            pact4sLogger.error(
              notWritingPactMessage(pact)
            )
          )
        } else {
          beforeWritePacts() >>
            IO.fromEither(verifyResultAndWritePactFiles(s))
        }
      }
    }
  }

  def pactTest(name: String)(test: BaseMockServer => Any)(implicit loc: Location): Unit =
    this.test(name)(test(serverFixture.apply()))

  override def test(options: TestOptions)(body: => Any)(implicit loc: Location): Unit =
    munitTestsBuffer += munitTestTransform(
      new Test(
        options.name,
        () =>
          try PlatformCompat.waitAtMost(() => munitValueTransform(body), munitTimeout, munitExecutionContext)
          catch {
            case NonFatal(e) =>
              testFailed = true
              Future.failed(e)
          },
        options.tags,
        loc
      )
    )

  override private[pact4s] type Effect[_] = IO[_]

  def beforeWritePacts(): IO[Unit] = IO.unit
}
