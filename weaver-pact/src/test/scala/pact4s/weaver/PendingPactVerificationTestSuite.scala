package pact4s.weaver

import pact4s.PendingPactVerificationFixture
import weaver.SimpleIOSuite

object PendingPactVerificationTestSuite extends SimpleIOSuite with PactVerifier with PendingPactVerificationFixture {
  pureTest("pending pact failure should be skipped")(succeed(verifyPacts()))
}
