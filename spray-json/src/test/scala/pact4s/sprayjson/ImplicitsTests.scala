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
import pact4s.sprayjson.implicits.providerStateFormat
import pact4s.provider.ProviderState
import spray.json._

class ImplicitsTests extends FunSuite {

  private def checkJsonRead(json: String, expectedProviderState: ProviderState): Unit = {
    println(JsonParser(json))
    println(expectedProviderState)
     assertEquals(JsonParser(json).convertTo[ProviderState], expectedProviderState)
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
