package pact4s

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.core.model.{BasePact, PactSpecVersion}
import au.com.dius.pact.core.support.V4PactFeaturesException

import scala.jdk.CollectionConverters.ListHasAsScala

trait BasePactForgerResources[Pact <: BasePact] {
  private[pact4s] val logger = org.log4s.getLogger

  def pact: Pact

  val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext()

  private[pact4s] def validatePactVersion(version: PactSpecVersion): Option[Throwable] = {
    val errors: List[String] = pact.validateForVersion(version).asScala.toList
    if (errors.isEmpty) None
    else {
      Some(
        new V4PactFeaturesException(
          "Pact specification V4 features can not be used with version " + version.toString + " - " + errors
            .mkString(", ")
        )
      )
    }
  }
}
