package pact4s.munit

import au.com.dius.pact.consumer.BaseMockServer
import cats.effect.{IO, Resource}
import munit.CatsEffectSuite
import pact4s.PactForgerResources

trait PactForger extends CatsEffectSuite with PactForgerResources {

  override def munitFixtures: Seq[Fixture[_]] = serverFixture +: additionalMunitFixtures

  def additionalMunitFixtures: Seq[Fixture[_]] = Seq.empty

  private val serverFixture: Fixture[BaseMockServer] = ResourceSuiteLocalFixture(
    "mockHttpServer",
    serverResource
  )

  private def serverResource: Resource[IO, BaseMockServer] = Resource.make[IO, BaseMockServer] {
    for {
      _ <- validatePactVersion.fold(IO.unit)(IO.raiseError)
      _ <- IO.delay(server.start())
      _ <- IO.delay(server.waitForServer())
    } yield server
  } { s =>
    logger.info(s"Writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}")
    IO.delay(s.verifyResultAndWritePact(null, pactTestExecutionContext, pact, mockProviderConfig.getPactVersion)) >>
      IO.delay(s.stop())
  }

  def pactTest(name: String)(test: BaseMockServer => Any): Unit = this.test(name)(test(serverFixture.apply()))
}