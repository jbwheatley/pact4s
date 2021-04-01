package munit.pact

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.{BaseMockServer, MockHttpServerKt, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.support.V4PactFeaturesException
import cats.effect.{IO, Resource}
import munit.CatsEffectSuite

import scala.jdk.CollectionConverters.ListHasAsScala

trait PactForger extends CatsEffectSuite {

  private val logger = org.log4s.getLogger

  def pact: RequestResponsePact

  val mockProviderConfig: MockProviderConfig = MockProviderConfig.createDefault()
  val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext()

  protected override def munitFixtures: Seq[Fixture[_]] = serverFixture +: additionalMunitFixtures

  def additionalMunitFixtures: Seq[Fixture[_]] = Seq.empty

  private def validatePactVersion: Option[Throwable] = {
    val errors: List[String] = pact.validateForVersion(mockProviderConfig.getPactVersion).asScala.toList
    if (errors.isEmpty) None else {
      Some(new V4PactFeaturesException("Pact specification V4 features can not be used with version " + mockProviderConfig.getPactVersion.toString + " - " + errors.mkString(", ")))
    }
  }

  private val serverFixture: Fixture[BaseMockServer] = ResourceSuiteLocalFixture(
    "mockHttpServer",
    server
  )

  private def server: Resource[IO, BaseMockServer] = Resource.make[IO, BaseMockServer] {
    val server: BaseMockServer = MockHttpServerKt.mockServer(pact, mockProviderConfig)
    for {
      _ <- IO.delay(server.start())
      _ <- IO.delay(server.waitForServer())
      _ <- validatePactVersion.fold(IO.unit)(IO.raiseError)
    } yield server
  } { s =>
    logger.info(s"Writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}")
    IO.delay(s.verifyResultAndWritePact(null, pactTestExecutionContext, pact, mockProviderConfig.getPactVersion)) >>
      IO.delay(s.stop())
  }

  def pactTest(name: String)(test: BaseMockServer => Any): Unit = this.test(name)(test(serverFixture.apply()))
}