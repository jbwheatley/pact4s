package http.provider

import cats.effect.kernel.Concurrent
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

final case class Resource(id: String, value: Int)

object Resource {
  implicit val encoder: Encoder[Resource] = Encoder.instance { res =>
    Json.obj(
      "id"    -> res.id.asJson,
      "value" -> res.value.asJson
    )
  }

  implicit def entityEncoder[F[_]]: EntityEncoder[F, Resource] = jsonEncoderOf[Resource]

  implicit val decoder: Decoder[Resource] = Decoder.forProduct2("id", "value")(Resource.apply)

  implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, Resource] = jsonOf[F, Resource]
}
