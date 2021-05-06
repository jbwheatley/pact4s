package pact4s

import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.model.PactSpecVersion

trait MessagePactForgerResources extends BasePactForgerResources[MessagePact] {
  val pactSpecVersion: PactSpecVersion = PactSpecVersion.V3
}
