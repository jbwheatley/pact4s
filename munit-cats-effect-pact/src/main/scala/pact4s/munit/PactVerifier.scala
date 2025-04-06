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

import au.com.dius.pact.provider.VerificationResult
import cats.effect.IO
import cats.implicits._
import munit.internal.console.Printers
import munit.{CatsEffectSuite, Location}
import pact4s.PactVerifyResources
import pact4s.effect.MonadLike
import sourcecode.{File, FileName, Line}

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

trait PactVerifier extends PactVerifyResources[IO] { self: CatsEffectSuite =>
  override private[pact4s] def skip(message: String)(implicit fileName: FileName, file: File, line: Line): IO[Unit] = {
    implicit val loc = new Location(file.value, line.value)
    Printers.log(message)
    IO(assume(cond = false, message))
  }

  override private[pact4s] implicit def F: MonadLike[IO] = new MonadLike[IO] {
    override def apply[A](a: => A): IO[A]                            = IO(a)
    override def map[A, B](a: => IO[A])(f: A => B): IO[B]            = a.map(f)
    override def flatMap[A, B](a: => IO[A])(f: A => IO[B]): IO[B]    = a.flatMap(f)
    override def foreach[A](as: List[A])(f: A => IO[Unit]): IO[Unit] = as.traverse_(f)
  }

  override private[pact4s] def failure(
      message: String
  )(implicit fileName: FileName, file: File, line: Line): IO[Nothing] = {
    implicit val loc: Location = new Location(file.value, line.value)
    IO(fail(message))
  }

  override private[pact4s] def runWithTimeout(
      verify: => IO[VerificationResult],
      timeout: Option[FiniteDuration]
  ): IO[Either[TimeoutException, VerificationResult]] =
    timeout match {
      case Some(timeout) =>
        verify
          .map(Right(_))
          .timeoutTo(timeout, IO(Left(new TimeoutException(s"verifying pacts timed out after $timeout"))))
      case None => verify.map(Right(_))
    }
}
