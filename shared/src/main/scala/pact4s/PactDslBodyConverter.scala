package pact4s

import au.com.dius.pact.consumer.dsl.PactDslJsonBody

trait PactDslJsonBodyEncoder[A] {
  def toPactDslJsonBody(a: A): PactDslJsonBody
}
