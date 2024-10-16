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

package http.provider

import cats.Functor
import cats.effect.Ref
import cats.effect.kernel.Sync
import cats.implicits._

class MockResourceStore[F[_]: Functor](ref: Ref[F, Map[String, Int]]) {
  def fetch(id: String): F[Option[ProviderResource]] = ref.get.map(_.get(id).map(i => ProviderResource(id, i)))

  def create(r: ProviderResource): F[Int] = ref.modify(rs =>
    rs.get(r.id) match {
      case Some(_) => (rs, 0)
      case None    => (rs + (r.id -> r.value), 1)
    }
  )

  def empty: F[Unit] = ref.set(Map.empty)
}

object MockResourceStore {
  def unsafe[F[_]: Sync](init: Map[String, Int] = Map.empty) = new MockResourceStore[F](Ref.unsafe(init))
}
