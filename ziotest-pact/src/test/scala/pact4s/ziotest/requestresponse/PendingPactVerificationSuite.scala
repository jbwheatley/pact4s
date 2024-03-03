package pact4s.ziotest.requestresponse

import pact4s.PendingPactVerificationFixture
import pact4s.ziotest.PactVerifier
import zio.ZIO
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object PendingPactVerificationSuite extends ZIOSpecDefault with PactVerifier with PendingPactVerificationFixture {
  override def spec: Spec[Any, Throwable] = test("pending pact failure") {
    ZIO.attempt(verifyPacts()).as(assertTrue(true))
  }

}
