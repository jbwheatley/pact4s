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

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.core.model.RequestResponseInteraction
import izumi.reflect.Tag
import pact4s.RequestResponsePactForgerResources
import zio.test.{Spec, TestAspect, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, UIO, ZIO, ZLayer}

import scala.annotation.nowarn

/** For when additional layers are required, e.g. a http client
  */
abstract class RequestResponsePactForgerWith[Resources: Tag]
    extends RequestResponsePactForgerBase[Resources with BaseMockServer] {
  def resources: ZLayer[Scope, Throwable, Resources]

  override private[pact4s] def _resources: ZLayer[Scope, Throwable, Resources with BaseMockServer] =
    resources ++ mockServer

}

trait RequestResponsePactForger extends RequestResponsePactForgerBase[BaseMockServer] {
  override private[pact4s] def _resources: ZLayer[Scope, Throwable, BaseMockServer] = mockServer
}

abstract class RequestResponsePactForgerBase[Resources: Tag]
    extends ZIOSpecDefault
    with RequestResponsePactForgerResources {

  def specName: String = s"Pact: ${pact.getConsumer.getName} - ${pact.getProvider.getName}"

  private var allTestsSucceeded: Boolean = false

  def verify(interaction: RequestResponseInteraction): Spec[Resources, Nothing] =
    interaction.getDescription match {
      case description => test(s"Missing verification for message: '$description'")(assertTrue(false))
    }

  private[pact4s] def mockServer: ZLayer[Scope, Throwable, BaseMockServer] = ZLayer.fromZIO {
    ZIO.acquireRelease(
      for {
        _          <- ZIO.fromEither(validatePactVersion(mockProviderConfig.getPactVersion))
        mockServer <- ZIO.attempt(createServer)
        _          <- ZIO.attempt(mockServer.start())
      } yield mockServer
    )(mockServer =>
      for {
        _ <- ZIO.attempt(mockServer.stop()).catchAll(e => ZIO.logError(s"failed to stop mock server: $e"))
        _ <-
          (beforeWritePacts() *> ZIO
            .attempt(verifyResultAndWritePactFiles(mockServer))
            .catchAll(e => ZIO.logError(s"failed to write pacts: $e"))).when(allTestsSucceeded): @nowarn
      } yield ()
    )
  }

  private[pact4s] def _resources: ZLayer[Scope, Throwable, Resources]

  def tests: Seq[Spec[Resources, Nothing]] = interactions.map(verify)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    (suite(specName)(tests) @@ TestAspect.afterAllSuccess(ZIO.succeed { allTestsSucceeded = true }))
      .provideSomeLayerShared[TestEnvironment with Scope](_resources)

  override private[pact4s] type Effect[A] = ZIO[Any, Throwable, A]

  override def beforeWritePacts(): UIO[Unit] = ZIO.unit
}
