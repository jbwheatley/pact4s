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

package pact4s.effect

// To avoid needing a cats dependency (or any other effect library)
trait MonadLike[F[_]] {
  def apply[A](a: => A): F[A]
  def map[A, B](a: => F[A])(f: A => B): F[B]
  def flatMap[A, B](a: => F[A])(f: A => F[B]): F[B]
  def foreach[A](as: List[A])(f: A => F[Unit]): F[Unit]
}

object MonadLike {
  def apply[F[_]](implicit ev: MonadLike[F]): MonadLike[F] = ev

  implicit val idMonadLike: MonadLike[Id] = new MonadLike[Id] {
    override def apply[A](a: => A): Id[A]                            = a
    override def map[A, B](a: => Id[A])(f: A => B): Id[B]            = f(a)
    override def flatMap[A, B](a: => Id[A])(f: A => Id[B]): Id[B]    = map(a)(f)
    override def foreach[A](as: List[A])(f: A => Id[Unit]): Id[Unit] = as.foreach(f)
  }

  private[pact4s] implicit class MonadSyntax[F[_]: MonadLike, A](a: => F[A]) {
    def map[B](f: A => B): F[B]        = MonadLike[F].map(a)(f)
    def flatMap[B](f: A => F[B]): F[B] = MonadLike[F].flatMap(a)(f)
  }
}
