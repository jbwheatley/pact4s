/*
 * Copyright 2021-2021 io.github.jbwheatley
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

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.core.model.messaging.Message
import cats.implicits.toBifunctorOps
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import pact4s.circe.JsonConversion.jsonToPactDslJsonBody
import pact4s.{MessagePactDecoder, PactBodyEncoder, PactDslJsonBodyEncoder}

object implicits {
  implicit def pactBodyEncoder[A](implicit encoder: Encoder[A]): PactBodyEncoder[A] =
    (a: A) => encoder(a).noSpaces

  implicit def pactDslJsonBodyConverter[A](implicit encoder: Encoder[A]): PactDslJsonBodyEncoder[A] = (a: A) =>
    jsonToPactDslJsonBody(encoder(a))

  implicit def messagePactDecoder[A](implicit decoder: Decoder[A]): MessagePactDecoder[A] = (message: Message) =>
    parse(message.contentsAsString()).leftWiden[Throwable].flatMap(j => decoder.decodeJson(j))
}
