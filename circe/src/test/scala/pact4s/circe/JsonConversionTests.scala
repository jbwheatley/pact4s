package pact4s.circe

import io.circe.Json
import io.circe.syntax.EncoderOps
import io.circe.parser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.circe.JsonConversion.jsonToPactDslJsonBody

class JsonConversionTests extends AnyFlatSpec with Matchers {
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

  it should "round trip" in {
    parser
      .parse(
        jsonToPactDslJsonBody(json).getBody
          .prettyPrint(0, true)
      )
      .getOrElse(fail("not valid JSON")) shouldBe json
  }
}
