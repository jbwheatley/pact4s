package pact4s.weaver.requestresponse

import pact4s.PendingPactVerificationFixture
import pact4s.weaver.PactVerifier
import weaver.SimpleIOSuite

object PendingPactVerificationSuite extends SimpleIOSuite with PactVerifier with PendingPactVerificationFixture {
  pureTest("pending pact failure should be skipped")(succeed(verifyPacts()))
}
