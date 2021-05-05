package pact4s

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.BasePact

trait BasePactForgerResources[Pact <: BasePact] {
  private[pact4s] val logger = org.log4s.getLogger

  def pact: Pact

  val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext()

  private[pact4s] def validatePactVersion: Option[Throwable]
}
