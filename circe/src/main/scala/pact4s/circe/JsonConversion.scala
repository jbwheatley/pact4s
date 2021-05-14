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
import io.circe.{Json, JsonObject}

private[circe] object JsonConversion {
  private val arrayError = "Auto-conversion of circe.Json to PactDslJsonBody is unsupported for json containing arrays."

  private def addFieldToBuilder(builder: PactDslJsonBody, fieldName: String, json: Json): PactDslJsonBody =
    json.fold[PactDslJsonBody](
      jsonNull = builder.nullValue(fieldName),
      jsonBoolean = bool => builder.booleanValue(fieldName, bool),
      jsonNumber = num =>
        builder
          .numberValue(fieldName, num.toLong.map(_.asInstanceOf[Number]).getOrElse(num.toDouble.asInstanceOf[Number])),
      jsonString = str => builder.stringValue(fieldName, str),
      jsonArray = _ => throw new Exception(arrayError),
      jsonObject = jsonObject => builder.`object`(fieldName, addJsonObjToBuilder(new PactDslJsonBody(), jsonObject))
    )

  private def addJsonObjToBuilder(builder: PactDslJsonBody, jsonObj: JsonObject): PactDslJsonBody =
    jsonObj.toMap.foldLeft(builder) { case (b, (s, j)) =>
      addFieldToBuilder(b, s, j)
    }

  private def _jsonToPactDslJsonBody(builder: PactDslJsonBody, json: Json): PactDslJsonBody =
    json.arrayOrObject(
      builder,
      _ => throw new Exception(arrayError),
      jObj =>
        jObj.toMap.foldLeft(builder) { case (b, (s, j)) =>
          addFieldToBuilder(b, s, j)
        }
    )

  def jsonToPactDslJsonBody(json: Json): PactDslJsonBody =
    _jsonToPactDslJsonBody(new PactDslJsonBody(), json)
}
