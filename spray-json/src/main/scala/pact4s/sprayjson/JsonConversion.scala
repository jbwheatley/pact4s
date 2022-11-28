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

import au.com.dius.pact.consumer.dsl.{DslPart, PactDslJsonArray, PactDslJsonBody, PactDslJsonRootValue}
import spray.json._

private[sprayjson] object JsonConversion {

  private def addFieldToBuilder(builder: PactDslJsonBody, fieldName: String, json: JsValue): PactDslJsonBody =
    json match {
      case JsNull               => builder.nullValue(fieldName)
      case JsTrue               => builder.booleanValue(fieldName, true)
      case JsFalse              => builder.booleanValue(fieldName, false)
      case JsNumber(num)        => builder.numberValue(fieldName, num)
      case JsString(str)        => builder.stringValue(fieldName, str)
      case JsArray(array)       => addArrayToJsonBody(builder, fieldName, array)
      case jsonObject: JsObject => builder.`object`(fieldName, addJsonObjToBuilder(new PactDslJsonBody(), jsonObject))
    }

  private def addJsonObjToBuilder(builder: PactDslJsonBody, jsonObj: JsObject): PactDslJsonBody =
    jsonObj.fields.foldLeft(builder) { case (b, (s, j)) =>
      addFieldToBuilder(b, s, j)
    }

  private def addArrayToJsonBody(builder: PactDslJsonBody, fieldName: String, array: Seq[JsValue]): PactDslJsonBody =
    addArrayValuesToArray(builder.array(fieldName), array).closeArray().asBody()

  private def addArrayValuesToArray(builder: PactDslJsonArray, array: Seq[JsValue]): PactDslJsonArray =
    array
      .foldLeft(builder) { (arrayBody, json) =>
        json match {
          case JsNull            => arrayBody.nullValue()
          case JsTrue            => arrayBody.booleanValue(true)
          case JsFalse           => arrayBody.booleanValue(false)
          case JsNumber(num)     => arrayBody.numberValue(num)
          case JsString(str)     => arrayBody.stringValue(str)
          case JsArray(arr)      => addArrayValuesToArray(arrayBody.array(), arr).closeArray().asArray()
          case jsonObj: JsObject => addJsonObjToBuilder(arrayBody.`object`(), jsonObj).closeObject().asArray()
        }
      }

  def jsonToPactDslJsonBody(json: JsValue): DslPart =
    json match {
      case JsNull            => throw new IllegalArgumentException("Content cannot be null json value if set")
      case JsFalse           => PactDslJsonRootValue.booleanType(false)
      case JsTrue            => PactDslJsonRootValue.booleanType(true)
      case JsNumber(num)     => PactDslJsonRootValue.numberType(num)
      case JsString(str)     => PactDslJsonRootValue.stringType(str)
      case JsArray(arr)      => addArrayValuesToArray(new PactDslJsonArray(), arr)
      case jsonObj: JsObject => addJsonObjToBuilder(new PactDslJsonBody(), jsonObj)
    }
}
