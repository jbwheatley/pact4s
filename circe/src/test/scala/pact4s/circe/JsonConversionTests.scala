package pact4s.circe

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.circe.parser
import munit.CatsEffectSuite
import pact4s.circe.JsonConversion.jsonToPactDslJsonBody

import scala.util.Try

class JsonConversionTests extends CatsEffectSuite {
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

    IO.fromEither(
      parser
        .parse(
          jsonToPactDslJsonBody(json).getBody
            .prettyPrint(0, true)
        )
    ).assertEquals(json)
  }

  test("an exception is raised if JSON is an array") {
    val json = Json.arr(
      Json.obj()
    )
    assert(Try(jsonToPactDslJsonBody(json)).isFailure)
  }

  test("an exception is raised if JSON contains a nested array") {
    val json = Json.obj(
      "array" -> List(1, 2, 3).asJson
    )
    assert(Try(jsonToPactDslJsonBody(json)).isFailure)
  }
}
