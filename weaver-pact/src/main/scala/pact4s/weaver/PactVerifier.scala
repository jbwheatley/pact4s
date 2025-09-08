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

import au.com.dius.pact.provider.VerificationResult
import cats.data.NonEmptyList
import cats.implicits._
import pact4s.PactVerifyResources
import pact4s.effect.MonadLike
import sourcecode.{File, FileName, Line}
import weaver.{AssertionException, CanceledException, MutableFSuite, SourceLocation}

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

trait PactVerifier[F[+_]] extends MutableFSuite[F] with PactVerifyResources[F] {
  override private[pact4s] def skip(
      message: String
  )(implicit fileName: FileName, file: File, line: Line): F[Unit] =
    effect.raiseError(new CanceledException(Some(message), SourceLocation(file.value, fileName.value, line.value, None)))
  override private[pact4s] def failure(
      message: String
  )(implicit fileName: FileName, file: File, line: Line): F[Nothing] =
    effect.raiseError(
      AssertionException(message, NonEmptyList.of(SourceLocation(file.value, fileName.value, line.value, None)))
    )

  override private[pact4s] implicit def F: MonadLike[F] = new MonadLike[F] {
    override def apply[A](a: => A): F[A]                           = effect.delay(a)
    override def map[A, B](a: => F[A])(f: A => B): F[B]            = a.map(f)
    override def flatMap[A, B](a: => F[A])(f: A => F[B]): F[B]     = a.flatMap(f)
    override def foreach[A](as: List[A])(f: A => F[Unit]): F[Unit] = as.traverse_(f)
  }

  override private[pact4s] def runWithTimeout(
      verify: => F[VerificationResult],
      timeout: Option[FiniteDuration]
  ): F[Either[TimeoutException, VerificationResult]] =
    timeout match {
      case Some(timeout) =>
        effect.timeoutTo(
          verify.map(Right(_)),
          timeout,
          effect.delay(Left(new TimeoutException(s"verifying pacts timed out after $timeout")))
        )
      case None => verify.map(Right(_))
    }
}
