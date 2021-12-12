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
