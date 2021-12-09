package pact4s.playjson

import au.com.dius.pact.core.model.messaging.Message
import pact4s.algebras.{MessagePactDecoder, PactBodyJsonEncoder, PactDslJsonBodyEncoder}
import pact4s.playjson.JsonConversion.jsonToPactDslJsonBody
import pact4s.provider.ProviderState
import play.api.libs.json.{Format, Json, Reads, Writes}

import scala.util.Try

object implicits {
  implicit def pactBodyEncoder[A](implicit writes: Writes[A]): PactBodyJsonEncoder[A] =
    (a: A) => Json.toJson(a).toString()

  implicit def pactDslJsonBodyConverter[A](implicit writes: Writes[A]): PactDslJsonBodyEncoder[A] = (a: A) =>
    jsonToPactDslJsonBody(Json.toJson(a))

  implicit def messagePactDecoder[A](implicit reads: Reads[A]): MessagePactDecoder[A] = (message: Message) =>
    Try(Json.parse(message.contentsAsString()).as[A]).toEither

  implicit val providerStateFormat: Format[ProviderState] = Json.format[ProviderState]
}
