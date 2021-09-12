package pact4s.scalatest
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.PendingPactVerificationFixture

class PendingPactVerificationTestSuite extends AnyFlatSpec with PactVerifier with PendingPactVerificationFixture {
  "pending pact failure" should "be skipped" in verifyPacts()
}
