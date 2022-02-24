package pact4s.munit.requestresponse

import munit.CatsEffectSuite
import pact4s.PendingPactVerificationFixture
import pact4s.munit.PactVerifier

class PendingPactVerificationSuite extends CatsEffectSuite with PactVerifier with PendingPactVerificationFixture {
  test("pending pact failure should be skipped")(verifyPacts())
}
