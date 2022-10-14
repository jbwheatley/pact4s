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
