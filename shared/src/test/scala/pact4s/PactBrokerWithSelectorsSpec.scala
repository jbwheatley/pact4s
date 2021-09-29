package pact4s

import munit.CatsEffectSuite
import pact4s.PactSource.PactBrokerWithSelectors
import pact4s.PactSource.PactBrokerWithSelectors.WipPactsSince

import java.time.Instant

class PactBrokerWithSelectorsSpec extends CatsEffectSuite {
  test("pending enabled but no provider tags should throw IllegalArgumentException") {
    intercept[IllegalArgumentException](PactBrokerWithSelectors("brokerUrl").withOptionalProviderTags(None))
  }

  test("pending disabled but WIP enabled should throw IllegalArgumentException") {
    intercept[IllegalArgumentException](
      PactBrokerWithSelectors("brokerUrl").withPendingPactsDisabled.withWipPactsSince(
        WipPactsSince.instant(Instant.now)
      )
    )
  }

  test("pending disabled but giving provider tags set should re-enable pending") {
    val p = PactBrokerWithSelectors("brokerUrl").withPendingPacts(false).withProviderTags(ProviderTags.one("TAG"))
    assert(p.enablePending)
  }
}
