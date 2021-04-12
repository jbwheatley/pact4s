package pact4s

import au.com.dius.pact.consumer.{BaseMockServer, MockHttpServerKt, PactTestExecutionContext}
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.support.V4PactFeaturesException

import scala.jdk.CollectionConverters.ListHasAsScala

trait PactForgerResources {
  private[pact4s] val logger = org.log4s.getLogger

  def pact: RequestResponsePact

  val mockProviderConfig: MockProviderConfig = MockProviderConfig.createDefault()
  val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext()

  private[pact4s] val server: BaseMockServer = MockHttpServerKt.mockServer(pact, mockProviderConfig)

  private[pact4s] def validatePactVersion: Option[Throwable] = {
    val errors: List[String] = pact.validateForVersion(mockProviderConfig.getPactVersion).asScala.toList
    if (errors.isEmpty) None else {
      Some(new V4PactFeaturesException("Pact specification V4 features can not be used with version " + mockProviderConfig.getPactVersion.toString + " - " + errors.mkString(", ")))
    }
  }
}
