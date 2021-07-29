/*
 * Copyright 2021-2021 io.github.jbwheatley
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

import au.com.dius.pact.consumer.dsl.{PactDslResponse, PactDslWithState}

import scala.jdk.CollectionConverters._

object RequestResponsePactOpsForPlatform {
  class PactDslResponseOpsForPlatform(val builder: PactDslResponse) extends AnyVal {
    def `given`(state: String, params: Map[String, Any]): PactDslWithState = builder.`given`(state, params.asJava)

    def statusCodes(codes: List[Int]): PactDslResponse = builder.statusCodes(codes.map(Integer.valueOf).asJava)
  }

  class PactDslWithStateOps(val builder: PactDslWithState) extends AnyVal {
    def `given`(stateDesc: String, params: Map[String, Any]): PactDslWithState =
      builder.`given`(stateDesc, params.asJava)
  }
}
