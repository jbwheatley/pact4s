package http.provider

import cats.Functor
import cats.effect.Ref
import cats.effect.kernel.Sync
import cats.implicits._

class MockResourceStore[F[_]: Functor](ref: Ref[F, Map[String, Int]]) {
  def fetch(id: String): F[Option[Resource]] = ref.get.map(_.get(id).map(i => Resource(id, i)))

  def create(r: Resource): F[Int] = ref.modify(rs =>
    rs.get(r.id) match {
      case Some(_) => (rs, 0)
      case None    => (rs + (r.id -> r.value), 1)
    }
  )

  def empty: F[Unit] = ref.set(Map.empty)
}

object MockResourceStore {
  def unsafe[F[_]: Sync] = new MockResourceStore[F](Ref.unsafe(Map.empty))
}
