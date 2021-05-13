package pact4s.circe

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import io.circe.{Json, JsonObject}

object JsonConversion {
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

  private[pact4s] def jsonToPactDslJsonBody(json: Json): PactDslJsonBody =
    _jsonToPactDslJsonBody(new PactDslJsonBody(), json)
}
