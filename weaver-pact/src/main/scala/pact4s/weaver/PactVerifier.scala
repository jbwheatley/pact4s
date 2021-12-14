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

import cats.data.NonEmptyList
import cats.effect.Resource
import cats.implicits._
import pact4s.PactVerifyResources
import sourcecode.{File, FileName, Line}
import weaver.{AssertionException, CanceledException, MutableFSuite, SourceLocation}

private[pact4s] trait WeaverPactVerifier[F[_]] extends MutableFSuite[F] with PactVerifyResources {
  private val F = effect

  override private[pact4s] def skip(message: String)(implicit fileName: FileName, file: File, line: Line): Unit =
    throw new CanceledException(Some(message), SourceLocation(file.value, fileName.value, line.value))
  override private[pact4s] def failure(message: String)(implicit fileName: FileName, file: File, line: Line): Nothing =
    throw AssertionException(message, NonEmptyList.of(SourceLocation(file.value, fileName.value, line.value)))

  type Resources
  override type Res = (Resources, Unit)

  private def stateChangerResource: Resource[F, Unit] =
    Resource.make(F.pure(stateChanger.start()))(_ => F.pure(stateChanger.shutdown()))

  def additionalSharedResource: Resource[F, Resources]

  override def sharedResource: Resource[F, (Resources, Unit)] =
    (additionalSharedResource, stateChangerResource).tupled

}

trait PactVerifier[F[_]] extends WeaverPactVerifier[F] {

  /** Use [[PactVerifierWithResources]] or [[MessagePactVerifierWithResources]] to add additional resources to your test
    * suite.
    */
  final type Resources = Unit
  final def additionalSharedResource: Resource[F, Resources] = Resource.unit[F]
}

trait PactVerifierWithResources[F[_]] extends WeaverPactVerifier[F]

trait MessagePactVerifier[F[_]] extends PactVerifier[F] {
  def messages: ResponseFactory
  override def responseFactory: Option[ResponseFactory] = Some(messages)
}

trait MessagePactVerifierWithResources[F[_]] extends PactVerifierWithResources[F] {
  def messages: ResponseFactory
  override def responseFactory: Option[ResponseFactory] = Some(messages)
}
