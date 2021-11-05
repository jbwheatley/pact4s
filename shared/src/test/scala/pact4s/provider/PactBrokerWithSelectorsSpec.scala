package pact4s.provider

import munit.CatsEffectSuite
import pact4s.provider.PactSource.PactBrokerWithSelectors

import java.time.Instant

class PactBrokerWithSelectorsSpec extends CatsEffectSuite {
  test("pending enabled but no provider tags should throw IllegalArgumentException") {
    intercept[IllegalArgumentException](
      PactBrokerWithSelectors("brokerUrl").withPendingPacts(true).withOptionalProviderTags(None).validate()
    )
  }

  test("WIP enabled but no provider tags should throw IllegalArgumentException") {
    intercept[IllegalArgumentException](
      PactBrokerWithSelectors("brokerUrl")
        .withWipPactsSince(
          WipPactsSince.instant(Instant.now)
        )
        .validate()
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
