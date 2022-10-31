package pact4s
package provider

import munit.FunSuite
import pact4s.provider.PactSource.PactBrokerWithSelectors

import java.time.Instant

class PactBrokerWithSelectorsSpec extends FunSuite {
  test("pending enabled but no provider tags should throw IllegalArgumentException") {
    assert(
      PactBrokerWithSelectors("brokerUrl").withPendingPacts(true).withOptionalProviderTags(None).validate().isLeft
    )
  }

  test("WIP enabled but no provider tags should throw IllegalArgumentException") {
    assert(
      PactBrokerWithSelectors("brokerUrl")
        .withWipPactsSince(
          WipPactsSince.instant(Instant.now)
        )
        .validate()
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
    p.validate()
    assertEquals(p.includeWipPactsSince, WipPactsSince.never)
  }
}
