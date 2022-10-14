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
