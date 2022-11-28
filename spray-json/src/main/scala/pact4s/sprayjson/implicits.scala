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

package pact4s.sprayjson

import au.com.dius.pact.core.model.messaging.Message
import pact4s.algebras.{MessagePactDecoder, PactBodyJsonEncoder, PactDslJsonBodyEncoder}
import pact4s.sprayjson.JsonConversion.jsonToPactDslJsonBody
import pact4s.provider.ProviderState
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.util.Try

object implicits {
  implicit def pactBodyEncoder[A](implicit writes: JsonFormat[A]): PactBodyJsonEncoder[A] =
    (a: A) => a.toJson.toString()

  implicit def pactDslJsonBodyConverter[A](implicit writes: JsonFormat[A]): PactDslJsonBodyEncoder[A] = (a: A) =>
    jsonToPactDslJsonBody(a.toJson)

  implicit def messagePactDecoder[A](implicit reads: JsonFormat[A]): MessagePactDecoder[A] = (message: Message) =>
    Try(JsonParser(message.contentsAsString()).convertTo[A]).toEither

  implicit object providerStateFormat extends JsonFormat[ProviderState] {
    def jsonAsString(json: JsValue): Option[String] =
      json match {
        case JsNull               => None
        case JsTrue               => Some("true")
        case JsFalse              => Some("false")
        case JsNumber(num)        => Some(num.toString())
        case JsString(str)        => Some(str)
        case jsArr: JsArray       => Some(jsArr.toString())
        case jsonObject: JsObject => Some(jsonObject.toString())
      }

    override def write(obj: ProviderState): JsValue = ???

    override def read(value: JsValue): ProviderState = {
      val provideStateFields = value.asJsObject.fields
      val stringParams = provideStateFields
        .get("params")
        .map(
          _.convertTo[JsObject].fields
            .map { case (k, v) => k -> jsonAsString(v) }
            .collect { case (k, Some(v)) => k -> v }
        )
        .getOrElse(Map.empty)

      ProviderState(provideStateFields("state").convertTo[String], stringParams)
    }
  }
}
