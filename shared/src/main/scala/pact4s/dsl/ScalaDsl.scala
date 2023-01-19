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

package pact4s.dsl

import au.com.dius.pact.consumer.dsl.{DslPart, LambdaDsl, LambdaDslJsonArray, LambdaDslObject}

trait ScalaDsl {

  // Note: the body parameters below are typed as X => Any instead of of X => Unit
  // It's meant as a convenience for users so that they don't get the "discarded value" warning/error

  def newJsonObject(body: LambdaDslObject => Any): DslPart =
    LambdaDsl
      .newJsonBody { obj =>
        body.apply(obj)
        ()
      }
      .build()

  def newJsonArray(body: LambdaDslJsonArray => Any): DslPart =
    LambdaDsl
      .newJsonArray { array =>
        body.apply(array)
        ()
      }
      .build()

  implicit def toLambdaDslObjectOps(obj: LambdaDslObject): LambdaDslObjectOps = new LambdaDslObjectOps(obj)

  implicit def toLambdaDslJsonArrayOps(array: LambdaDslJsonArray): LambdaDslJsonArrayOps = new LambdaDslJsonArrayOps(
    array
  )

}

class LambdaDslObjectOps(val obj: LambdaDslObject) extends AnyVal {

  def newArray(name: String)(body: LambdaDslJsonArray => Any): LambdaDslObject =
    obj.array(
      name,
      { a =>
        body.apply(a)
        ()
      }
    )

  def newObject(name: String)(body: LambdaDslObject => Any): LambdaDslObject =
    obj.`object`(
      name,
      { o =>
        body.apply(o)
        ()
      }
    )

}

class LambdaDslJsonArrayOps(val array: LambdaDslJsonArray) extends AnyVal {

  def newArray(body: LambdaDslJsonArray => Any): LambdaDslJsonArray =
    array.array { a =>
      body.apply(a)
      ()
    }

  def newObject(body: LambdaDslObject => Any): LambdaDslJsonArray =
    array.`object` { o =>
      body.apply(o)
      ()
    }

}
