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
package syntax

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import pact4s.syntax.PactDslPartOps.PactDslJsonBodyOps

object PactDslPartOps {
  class PactDslJsonBodyOps(val builder: PactDslJsonBody) extends AnyVal {
    def intType(name: String, value: Int): PactDslJsonBody   = builder.integerType(name, value: Integer)
    def longType(name: String, value: Long): PactDslJsonBody = builder.integerType(name, value: java.lang.Long)
  }
}

trait PactDslPartOps {
  implicit def toPactDslJsonBodyOps(builder: PactDslJsonBody): PactDslJsonBodyOps =
    new PactDslJsonBodyOps(builder)
}
