package pact4s.circe

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.circe.parser
import munit.CatsEffectSuite
import pact4s.circe.JsonConversion.jsonToPactDslJsonBody

class JsonConversionTests extends CatsEffectSuite {
  def testRoundTrip(json: Json): IO[Unit] = IO
    .fromEither(
      parser
        .parse(
          jsonToPactDslJsonBody(json).getBody.toString
        )
    )
    .assertEquals(json)

  test("array-less JSON should round-trip with PactDslJsonBody") {
    val json = Json.obj(
      "key1" -> "value1".asJson,
      "key2" -> Json.obj(
        "key2.1" -> true.asJson,
        "key2.2" -> Json.Null,
        "key2.3" -> Json.obj()
      ),
      "key3" -> 1.asJson,
      "key4" -> 2.34.asJson
    )

    testRoundTrip(json)
  }

  test("should raise exception if json is a top-level array") {
    val json = Json.arr(
      1.asJson,
      2.asJson,
      3.asJson
    )
    testRoundTrip(json)
  }

  test("should roundtrip an empty json object") {
    testRoundTrip(Json.obj())
  }

  test("should work if JSON object contains a nested simple array") {
    val json = Json.obj(
      "array" -> List(1, 2, 3).asJson
    )
    testRoundTrip(json)
  }

  test("should work if JSON object contains a nested array of objects") {
    val json = Json.obj(
      "array" -> List(
        Json.obj("f" -> "g".asJson),
        Json.obj("f" -> "h".asJson)
      ).asJson
    )
    testRoundTrip(json)
  }

  test("should work if JSON object contains an array of array") {
    val json = Json.obj(
      "array" -> List(
        List(1, 2, 3).asJson,
        List(4, 5, 6).asJson
      ).asJson
    )
    testRoundTrip(json)
  }

  test("should encode top level string") {
    assertEquals(jsonToPactDslJsonBody("pact4s".asJson).getBody.asString(), "pact4s")
  }

  test("should encode top level boolean") {
    assertEquals(jsonToPactDslJsonBody(true.asJson).getBody.asBoolean().booleanValue(), true)
  }

  test("should encode top level number") {
    assertEquals(jsonToPactDslJsonBody(12.asJson).getBody.asNumber().intValue(), 12)
  }

  test("should encode top level null") {
    println(None.asJson.spaces2)
  }
}
