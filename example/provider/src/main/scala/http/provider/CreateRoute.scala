package http.provider

import cats.effect.kernel.Concurrent
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Authorization, `WWW-Authenticate`}
import org.http4s.{BasicCredentials, Challenge, HttpRoutes}

object CreateRoute {
  def apply[F[_]: Concurrent](createResource: Resource => F[Int], apiKey: String): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of { case req @ POST -> Root / "resource" =>
      req.headers.get[Authorization] match {
        case Some(Authorization(BasicCredentials("user", `apiKey`))) =>
          req.as[Resource].flatMap(createResource).flatMap {
            case 1 => NoContent()
            case 0 => Conflict()
            case _ => InternalServerError()
          }
        case _ => Unauthorized(`WWW-Authenticate`(Challenge("", "")))
      }

    }
  }
}
