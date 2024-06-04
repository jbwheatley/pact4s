package pact4s.weaver.requestresponse

import cats.effect.IO
import pact4s.PendingPactVerificationFixture
import pact4s.weaver.PactVerifier
import weaver.SimpleIOSuite

object PendingPactVerificationSuite
    extends SimpleIOSuite
    with PactVerifier[IO]
    with PendingPactVerificationFixture[IO] {
  test("pending pact failure should be skipped")(verifyPacts().map(succeed))
}
