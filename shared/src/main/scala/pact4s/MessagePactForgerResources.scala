package pact4s

import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.V4PactFeaturesException

import scala.jdk.CollectionConverters.CollectionHasAsScala

trait MessagePactForgerResources extends BasePactForgerResources[MessagePact] {
  val pactSpecVersion: PactSpecVersion = PactSpecVersion.V3

  private[pact4s] def validatePactVersion: Option[Throwable] = {
    val errors: List[String] = pact.validateForVersion(pactSpecVersion).asScala.toList
    if (errors.isEmpty) None
    else {
      Some(
        new V4PactFeaturesException(
          "Pact specification V4 features can not be used with version " + pactSpecVersion.toString + " - " + errors
            .mkString(", ")
        )
      )
    }
  }
}
