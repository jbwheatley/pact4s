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

import pact4s.PactVerifyResources
import pact4s.effect.MonadLike
import sourcecode.{File, FileName, Line}
import zio.{Task, ZIO}

trait PactVerifier extends PactVerifyResources[Task] {
  override private[pact4s] def skip(message: String)(implicit fileName: FileName, file: File, line: Line): Task[Unit] =
    ZIO.logInfo(message)

  override private[pact4s] def failure(
      message: String
  )(implicit fileName: FileName, file: File, line: Line): Task[Nothing] =
    ZIO.fail(new RuntimeException(message))

  override private[pact4s] implicit val F: MonadLike[Task] = new MonadLike[Task] {
    override def apply[A](a: => A): Task[A] = ZIO.succeed(a)

    override def map[A, B](a: => Task[A])(f: A => B): Task[B] = a.map(f)

    override def flatMap[A, B](a: => Task[A])(f: A => Task[B]): Task[B] = a.flatMap(f)

    override def foreach[A](as: List[A])(f: A => Task[Unit]): Task[Unit] = ZIO.foreachDiscard(as)(f)
  }

}
