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

import au.com.dius.pact.consumer.dsl.{DslPart, PactDslJsonArray, PactDslJsonBody, PactDslJsonRootValue}
import io.circe.{Json, JsonNumber, JsonObject}

private[circe] object JsonConversion {
  private def jsonNumberToNumber(num: JsonNumber): Number =
    num.toLong.map(_.asInstanceOf[Number]).getOrElse(num.toDouble.asInstanceOf[Number])

  private def addFieldToBuilder(builder: PactDslJsonBody, fieldName: String, json: Json): PactDslJsonBody =
    json.fold[PactDslJsonBody](
      jsonNull = builder.nullValue(fieldName),
      jsonBoolean = bool => builder.booleanValue(fieldName, bool),
      jsonNumber = num =>
        builder
          .numberValue(fieldName, jsonNumberToNumber(num)),
      jsonString = str => builder.stringValue(fieldName, str),
      jsonArray = array => addArrayToJsonBody(builder, fieldName, array),
      jsonObject = jsonObject => builder.`object`(fieldName, addJsonObjToBuilder(new PactDslJsonBody(), jsonObject))
    )

  private def addJsonObjToBuilder(builder: PactDslJsonBody, jsonObj: JsonObject): PactDslJsonBody =
    jsonObj.toMap.foldLeft(builder) { case (b, (s, j)) =>
      addFieldToBuilder(b, s, j)
    }

  private def addArrayToJsonBody(builder: PactDslJsonBody, fieldName: String, array: Vector[Json]): PactDslJsonBody =
    addArrayValuesToArray(builder.array(fieldName), array).closeArray().asBody()

  private def addArrayValuesToArray(builder: PactDslJsonArray, array: Vector[Json]): PactDslJsonArray =
    array
      .foldLeft(builder) { (arrayBody, json) =>
        json.fold[PactDslJsonArray](
          jsonNull = arrayBody.nullValue(),
          jsonBoolean = bool => arrayBody.booleanValue(bool),
          jsonNumber = num => arrayBody.numberValue(jsonNumberToNumber(num)),
          jsonString = str => arrayBody.stringValue(str),
          jsonArray = jArr => addArrayValuesToArray(arrayBody.array(), jArr).closeArray().asArray(),
          jsonObject = jObj => addJsonObjToBuilder(arrayBody.`object`(), jObj).closeObject().asArray()
        )
      }

  def jsonToPactDslJsonBody(json: Json): DslPart =
    json.fold(
      jsonNull = throw new IllegalArgumentException("Content cannot be null json value if set"),
      jsonBoolean = bool => PactDslJsonRootValue.booleanType(bool),
      jsonNumber = num => PactDslJsonRootValue.numberType(jsonNumberToNumber(num)),
      jsonString = str => PactDslJsonRootValue.stringType(str),
      jsonArray = jArr => addArrayValuesToArray(new PactDslJsonArray(), jArr),
      jsonObject = jObj => addJsonObjToBuilder(new PactDslJsonBody(), jObj)
    )
}
