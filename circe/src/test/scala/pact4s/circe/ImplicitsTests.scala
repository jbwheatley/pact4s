package pact4s.circe

import io.circe.parser.parse
import munit.FunSuite
import pact4s.circe.implicits.providerStateCodec
import pact4s.provider.ProviderState

class ImplicitsTests extends FunSuite {

  private def checkJsonRead(json: String, expectedProviderState: ProviderState): Unit =
    parse(json).flatMap(_.as[ProviderState]) match {
      case Right(actualProviderState) => assertEquals(actualProviderState, expectedProviderState)
      case Left(err)                  => fail(s"Parsing json failed", err)
    }

  test("ProviderState read no param") {
    checkJsonRead(
      """{"state": "some state"}""",
      ProviderState("some state", Map())
    )
  }

  test("ProviderState read string param") {
    checkJsonRead(
      """{"state": "some state", "params": {"someKey": "some string"}}""",
      ProviderState("some state", Map("someKey" -> "some string"))
    )
  }

  test("ProviderState read int param") {
    checkJsonRead(
      """{"state": "some state", "params": {"someKey": 42}}""",
      ProviderState("some state", Map("someKey" -> "42"))
    )
  }

  test("ProviderState read null param") {
    checkJsonRead(
      """{"state": "some state", "params": {"someKey": null}}""",
      ProviderState("some state", Map())
    )
  }

  test("ProviderState read boolean param") {
    checkJsonRead(
      """{"state": "some state", "params": {"someKey": true}}""",
      ProviderState("some state", Map("someKey" -> "true"))
    )
  }

  test("ProviderState read object param") {
    checkJsonRead(
      """{"state": "some state", "params": {"someKey": {"innerKey": "innerValue"}}}""",
      ProviderState("some state", Map("someKey" -> """{"innerKey":"innerValue"}"""))
    )
  }

  test("ProviderState read array param") {
    checkJsonRead(
      """{"state": "some state", "params": {"someKey": [1, 2, 3]}}""",
      ProviderState("some state", Map("someKey" -> """[1,2,3]"""))
    )
  }

}
