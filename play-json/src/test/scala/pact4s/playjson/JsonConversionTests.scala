package pact4s.playjson

import munit.FunSuite
import pact4s.playjson.JsonConversion.jsonToPactDslJsonBody
import play.api.libs.json.{JsNull, JsValue, Json}

class JsonConversionTests extends FunSuite {

  def testRoundTrip(json: JsValue): Unit =
    assertEquals(Json.parse(jsonToPactDslJsonBody(json).getBody.toString), json)

  test("array-less JSON should round-trip with PactDslJsonBody") {
    val json = Json.obj(
      "key1" -> Json.toJson("value1"),
      "key2" -> Json.obj(
        "key2.1" -> Json.toJson(true),
        "key2.2" -> JsNull,
        "key2.3" -> Json.obj()
      ),
      "key3" -> Json.toJson(1),
      "key4" -> Json.toJson(2.34)
    )

    testRoundTrip(json)
  }

  test("should raise exception if json is a top-level array") {
    val json = Json.arr(
      Json.toJson(1),
      Json.toJson(2),
      Json.toJson(3)
    )
    testRoundTrip(json)
  }

  test("should roundtrip an empty json object") {
    testRoundTrip(Json.obj())
  }

  test("should work if JSON object contains a nested simple array") {
    val json = Json.obj(
      "array" -> Json.toJson(List(1, 2, 3))
    )
    testRoundTrip(json)
  }

  test("should work if JSON object contains a nested array of objects") {
    val json = Json.obj(
      "array" -> Json.toJson(
        List(
          Json.obj("f" -> Json.toJson("g")),
          Json.obj("f" -> Json.toJson("h"))
        )
      )
    )
    testRoundTrip(json)
  }

  test("should work if JSON object contains an array of array") {
    val json = Json.obj(
      "array" -> Json.toJson(
        List(
          Json.toJson(List(1, 2, 3)),
          Json.toJson(List(4, 5, 6))
        )
      )
    )
    testRoundTrip(json)
  }

  test("should encode top level string") {
    assertEquals(jsonToPactDslJsonBody(Json.toJson("pact4s")).getBody.asInstanceOf[String], "pact4s")
  }

  test("should encode top level boolean") {
    assertEquals(jsonToPactDslJsonBody(Json.toJson(true)).getBody.asInstanceOf[Boolean], true)
  }

  test("should encode top level number") {
    assertEquals(jsonToPactDslJsonBody(Json.toJson(12)).getBody.asInstanceOf[BigDecimal].toInt, 12)
  }
}
