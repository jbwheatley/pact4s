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

package http.consumer

import cats.effect.kernel.Concurrent
import cats.syntax.all._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.http4s._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.headers.Authorization

trait ProviderClient[F[_]] {
  def fetchResource(id: String): F[Option[ProviderResource]]

  def createResource(resource: ProviderResource): F[Unit]
}

class ProviderClientImpl[F[_]: Concurrent](client: Client[F], baseUrl: Uri, creds: BasicCredentials)
    extends ProviderClient[F] {
  def fetchResource(id: String): F[Option[ProviderResource]] = {
    val request = Request[F](uri = baseUrl / "resource" / id).withHeaders(Authorization(creds))
    client.run(request).use { resp =>
      resp.status match {
        case Status.Ok           => resp.as[ProviderResource].map(_.some)
        case Status.NotFound     => none[ProviderResource].pure[F]
        case Status.Unauthorized => InvalidCredentials.raiseError
        case _                   => UnknownError.raiseError
      }
    }
  }

  def createResource(resource: ProviderResource): F[Unit] = {
    val request = Request[F](method = Method.POST, uri = baseUrl / "resource")
      .withHeaders(Authorization(creds))
      .withEntity(resource)
    client.run(request).use { resp =>
      resp.status match {
        case Status.NoContent => ().pure[F]
        case Status.Conflict  => UserAlreadyExists.raiseError
        case _                => UnknownError.raiseError
      }
    }
  }
}

final case class ProviderResource(id: String, value: Int)

object ProviderResource {
  implicit val encoder: Encoder[ProviderResource] = Encoder.instance { res =>
    Json.obj(
      "id"    -> res.id.asJson,
      "value" -> res.value.asJson
    )
  }

  implicit def entityEncoder[F[_]]: EntityEncoder[F, ProviderResource] = jsonEncoderOf[F, ProviderResource]

  implicit val decoder: Decoder[ProviderResource] = Decoder.forProduct2("id", "value")(ProviderResource.apply)

  implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, ProviderResource] = jsonOf[F, ProviderResource]
}

case object InvalidCredentials extends Exception
case object UserAlreadyExists  extends Exception
case object UnknownError       extends Exception
