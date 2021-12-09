package pact4s.playjson

import au.com.dius.pact.consumer.dsl.{DslPart, PactDslJsonArray, PactDslJsonBody, PactDslJsonRootValue}
import play.api.libs.json._

private[playjson] object JsonConversion {

  private def addFieldToBuilder(builder: PactDslJsonBody, fieldName: String, json: JsValue): PactDslJsonBody =
    json match {
      case JsNull               => builder.nullValue(fieldName)
      case JsTrue               => builder.booleanValue(fieldName, true)
      case JsFalse              => builder.booleanValue(fieldName, false)
      case JsNumber(num)        => builder.numberValue(fieldName, num)
      case JsString(str)        => builder.stringValue(fieldName, str)
      case JsArray(array)       => addArrayToJsonBody(builder, fieldName, array.toSeq)
      case jsonObject: JsObject => builder.`object`(fieldName, addJsonObjToBuilder(new PactDslJsonBody(), jsonObject))
    }

  private def addJsonObjToBuilder(builder: PactDslJsonBody, jsonObj: JsObject): PactDslJsonBody =
    jsonObj.value.foldLeft(builder) { case (b, (s, j)) =>
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
          case JsArray(arr)      => addArrayValuesToArray(arrayBody.array(), arr.toSeq).closeArray().asArray()
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
      case JsArray(arr)      => addArrayValuesToArray(new PactDslJsonArray(), arr.toSeq)
      case jsonObj: JsObject => addJsonObjToBuilder(new PactDslJsonBody(), jsonObj)
    }
}
