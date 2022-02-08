package http.provider

import cats.Monad
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Authorization, `WWW-Authenticate`}
import org.http4s.{BasicCredentials, Challenge, HttpRoutes}

object FetchRoute {
  def apply[F[_]: Monad](getResource: String => F[Option[Resource]], apiKey: String): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of { case req @ GET -> Root / "resource" / id =>
      req.headers.get[Authorization] match {
        case Some(Authorization(BasicCredentials("user", `apiKey`))) =>
          getResource(id).flatMap {
            case Some(r) => Ok(r)
            case None    => NotFound()
          }
        case _ => Unauthorized(`WWW-Authenticate`(Challenge("", "")))
      }

    }
  }
}
