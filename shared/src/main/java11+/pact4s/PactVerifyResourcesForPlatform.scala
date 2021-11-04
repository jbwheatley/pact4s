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

package pact4s

import au.com.dius.pact.provider.VerificationResult
import com.google.common.util.concurrent.SimpleTimeLimiter

import java.util.concurrent.{Callable, Executors, TimeUnit}
import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

trait PactVerifyResourcesForPlatform {
  private[pact4s] def runWithTimeout(
      verify: => VerificationResult,
      timeout: Option[FiniteDuration]
  ): Either[TimeoutException, VerificationResult] = timeout match {
    case Some(timeout) =>
      val runVerification = new Callable[VerificationResult] {
        def call(): VerificationResult = verify
      }
      try Right(
        SimpleTimeLimiter
          .create(Executors.newSingleThreadExecutor())
          .callWithTimeout(runVerification, timeout.toSeconds, TimeUnit.SECONDS)
      )
      catch {
        case e: TimeoutException => Left(e)
      }
    case None => Right(verify)
  }
}
