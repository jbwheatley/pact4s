package pact4s

import au.com.dius.pact.consumer.PactTestExecutionContext

trait PactTestExecutionContextAware {
  def pactTestExecutionContext: PactTestExecutionContext
}
