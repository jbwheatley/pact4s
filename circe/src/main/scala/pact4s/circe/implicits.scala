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

package pact4s.circe

import au.com.dius.pact.core.model.messaging.Message
import cats.implicits.toBifunctorOps
import io.circe.Decoder.Result
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import pact4s.algebras.{MessagePactDecoder, PactBodyJsonEncoder, PactDslJsonBodyEncoder}
import pact4s.circe.JsonConversion.jsonToPactDslJsonBody
import pact4s.provider.ProviderState

object implicits {
  implicit def pactBodyEncoder[A](implicit encoder: Encoder[A]): PactBodyJsonEncoder[A] =
    (a: A) => encoder(a).noSpaces

  implicit def pactDslJsonBodyConverter[A](implicit encoder: Encoder[A]): PactDslJsonBodyEncoder[A] = (a: A) =>
    jsonToPactDslJsonBody(encoder(a))

  implicit def messagePactDecoder[A](implicit decoder: Decoder[A]): MessagePactDecoder[A] = (message: Message) =>
    parse(message.contentsAsString()).leftWiden[Throwable].flatMap(j => decoder.decodeJson(j))

  implicit val providerStateCodec: Codec[ProviderState] = new Codec[ProviderState] {
    def apply(a: ProviderState): Json = Json.obj("state" -> a.state.asJson, "params" -> a.params.asJson)

    def apply(c: HCursor): Result[ProviderState] =
      for {
        state  <- c.get[String]("state")
        params <- c.get[Option[JsonObject]]("params")
        stringParams = params
          .map(
            _.toMap
              .map { case (k, v) => k -> jsonAsString(v) }
              .collect { case (k, Some(v)) => k -> v }
          )
          .getOrElse(Map.empty)
      } yield ProviderState(state, stringParams)

    private def jsonAsString(json: Json): Option[String] =
      json.fold[Option[String]](
        jsonNull = None,
        jsonBoolean = bool => Some(bool.toString),
        jsonNumber = num => Some(num.toString),
        jsonString = str => Some(str),
        jsonArray = arr => Some(arr.asJson.noSpaces),
        jsonObject = obj => Some(obj.asJson.noSpaces)
      )

  }
}
