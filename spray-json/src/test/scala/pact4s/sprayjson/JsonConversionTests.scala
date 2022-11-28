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

import munit.FunSuite
import pact4s.sprayjson.JsonConversion.jsonToPactDslJsonBody

import spray.json._
import spray.json.DefaultJsonProtocol._

class JsonConversionTests extends FunSuite {

  def testRoundTrip(json: JsValue): Unit =
    assertEquals(JsonParser(jsonToPactDslJsonBody(json).getBody.toString), json)

  test("array-less JSON should round-trip with PactDslJsonBody") {
    val json = JsObject(
      "key1" -> "value1".toJson,
      "key2" -> JsObject(
        "key2.1" -> true.toJson,
        "key2.2" -> JsNull,
        "key2.3" -> JsObject()
      ),
      "key3" -> 1.toJson,
      "key4" -> 2.34.toJson
    )

    testRoundTrip(json)
  }

  test("should raise exception if json is a top-level array") {
    val json = JsArray(
      1.toJson,
      2.toJson,
      3.toJson
    )
    testRoundTrip(json)
  }

  test("should roundtrip an empty json object") {
    testRoundTrip(JsObject())
  }

  test("should work if JSON object contains a nested simple array") {
    val json = JsObject(
      "array" -> List(1, 2, 3).toJson
    )
    testRoundTrip(json)
  }

  test("should work if JSON object contains a nested array of objects") {
    val json = JsObject(
      "array" ->
        List(
          JsObject("f" -> "g".toJson),
          JsObject("f" -> "h".toJson)
        ).toJson
    )
    testRoundTrip(json)
  }

  test("should work if JSON object contains an array of array") {
    val json = JsObject(
      "array" -> JsArray(
        Vector(
          List(1, 2, 3).toJson,
          List(4, 5, 6).toJson
        )
      )
    )
    testRoundTrip(json)
  }

  test("should encode top level string") {
    assertEquals(jsonToPactDslJsonBody("pact4s".toJson).getBody.asString(), "pact4s")
  }

  test("should encode top level boolean") {
    assertEquals(jsonToPactDslJsonBody(true.toJson).getBody.asBoolean().booleanValue(), true)
  }

  test("should encode top level number") {
    assertEquals(jsonToPactDslJsonBody(12.toJson).getBody.asNumber().intValue(), 12)
  }
}
