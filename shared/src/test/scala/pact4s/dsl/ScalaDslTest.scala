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

package pact4s
package dsl

import au.com.dius.pact.consumer.dsl.{DslPart, LambdaDsl}
import munit.FunSuite

class ScalaDslTest extends FunSuite {

  test("Array extension methods are consistent") {

    object TraditionalDsl {

      val dsl: DslPart = LambdaDsl
        .newJsonArray { rootArray =>
          rootArray.array { a =>
            a.stringValue("a1")
            a.stringValue("a2")
            ()
          }
          rootArray.array { a =>
            a.numberValue(1).numberValue(2)
            ()
          }
          rootArray.array { a =>
            a.`object` { o =>
              o.stringValue("foo", "Foo")
              ()
            }
            ()
          }
          ()
        }
        .build()

    }

    object NewDsl extends ScalaDsl {

      val dsl: DslPart = newJsonArray { rootArray =>
        rootArray.newArray { a =>
          a.stringValue("a1")
          a.stringValue("a2")
        }
        rootArray.newArray { a =>
          a.numberValue(1)
          a.numberValue(2)
        }
        rootArray.newArray { a =>
          a.newObject { o =>
            o.stringValue("foo", "Foo")
          }
        }
      }

    }

    assertDslEquals(NewDsl.dsl, TraditionalDsl.dsl)
  }

  test("Object extension methods are consistent") {

    object TraditionalDsl {

      val dsl: DslPart = LambdaDsl
        .newJsonBody { rootObj =>
          rootObj.`object`(
            "keyA",
            o => {
              o.stringType("a1")
              o.stringType("a2")
              ()
            }
          )
          rootObj.array(
            "keyB",
            a => {
              a.integerType()
              ()
            }
          )
          ()
        }
        .build()

    }

    object NewDsl extends ScalaDsl {

      val dsl: DslPart = newJsonObject { rootObj =>
        rootObj.newObject("keyA") { o =>
          o.stringType("a1")
          o.stringType("a2")
        }
        rootObj.newArray("keyB") { a =>
          a.integerType()
        }
      }

    }

    assertDslEquals(NewDsl.dsl, TraditionalDsl.dsl)

  }

  private def assertDslEquals(actualDsl: DslPart, expectedDsl: DslPart): Unit =
    // Comparing bodies gives a good level of confidence that both DSL are equivalent
    assertEquals(actualDsl.getBody, expectedDsl.getBody)

}
