package pact4s.munit

import munit.CatsEffectSuite
import pact4s.PendingPactVerificationFixture

class PendingPactVerificationTestSuite extends CatsEffectSuite with PactVerifier with PendingPactVerificationFixture {
  test("pending pact failure should be skipped")(verifyPacts())
}
