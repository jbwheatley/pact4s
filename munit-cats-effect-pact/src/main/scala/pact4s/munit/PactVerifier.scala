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
import munit.internal.console.Printers
import munit.{CatsEffectSuite, Location}
import pact4s.{Pact4sLogger, PactVerifyResources}
import sourcecode.{File, FileName, Line}

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

trait PactVerifier extends PactVerifyResources with Pact4sLogger { self: CatsEffectSuite =>
  override private[pact4s] def skip(message: String)(implicit fileName: FileName, file: File, line: Line): Unit = {
    implicit val loc = new Location(file.value, line.value)
    Printers.log(message)
    assume(cond = false, message)
  }

  override private[pact4s] def failure(message: String)(implicit fileName: FileName, file: File, line: Line): Nothing =
    fail(message)(new Location(file.value, line.value))

  override private[pact4s] def runWithTimeout(
      verify: () => VerificationResult,
      timeout: Option[FiniteDuration]
  ): Either[TimeoutException, VerificationResult] =
    timeout match {
      case Some(timeout) =>
        IO.delay(verify())
          .timeout(timeout)
          .attempt
          .flatMap[Either[TimeoutException, VerificationResult]] {
            case Left(ex: TimeoutException) => IO(Left(ex))
            case Left(ex)                   => IO.raiseError(ex)
            case Right(value)               => IO(Right(value))
          }
          .unsafeRunSync()
      case None => Right(verify())
    }
}

trait MessagePactVerifier extends PactVerifier { _: CatsEffectSuite =>
  def messages: ResponseFactory
  override def responseFactory: Option[ResponseFactory] = Some(messages)
}
