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
package provider

import munit.FunSuite
import pact4s.provider.PactSource.PactBrokerWithSelectors

import java.time.Instant
import scala.annotation.nowarn

@nowarn
class PactBrokerWithSelectorsSpec extends FunSuite {

  test("pending enabled but no provider branch should throw IllegalArgumentException") {
    assert(
      PactBrokerWithSelectors("brokerUrl").withPendingPacts(true).validate(None).isLeft
    )
  }

  test("pending enabled but no provider tags nor branch should throw IllegalArgumentException") {
    assert(
      PactBrokerWithSelectors("brokerUrl").withPendingPacts(true).withOptionalProviderTags(None).validate(None).isLeft
    )
  }

  test("WIP enabled but no provider tags nor branch should throw IllegalArgumentException") {
    assert(
      PactBrokerWithSelectors("brokerUrl")
        .withWipPactsSince(
          WipPactsSince.instant(Instant.now)
        )
        .validate(None)
        .isLeft
    )
  }

  test("WIP enabled should turn on pending") {
    val p = PactBrokerWithSelectors("brokerUrl").withWipPactsSince(
      WipPactsSince.instant(Instant.now)
    )
    assert(p.enablePending)
  }

  test("withPendingPacts(false) should turn off WIP pacts") {
    val p = PactBrokerWithSelectors("brokerUrl")
      .withWipPactsSince(
        WipPactsSince.instant(Instant.now)
      )
      .withPendingPacts(false)
    p.validate(None)
    assertEquals(p.includeWipPactsSince, WipPactsSince.never)
  }

}
