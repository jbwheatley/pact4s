package pact4s.weaver

import cats.effect.IO
import pact4s.PendingPactVerificationFixture
import weaver.IOSuite

object PendingPactVerificationTestSuite extends IOSuite with PactVerifier[IO] with PendingPactVerificationFixture {
  pureTest("pending pact failure should be skipped")(succeed(verifyPacts()))
}
