package pact4s.munit

import au.com.dius.pact.consumer.BaseMockServer
import cats.effect.{IO, Resource}
import munit.internal.PlatformCompat
import munit.{CatsEffectSuite, Location, TestOptions}
import pact4s.PactForgerResources

import scala.concurrent.Future
import scala.util.control.NonFatal

trait PactForger extends CatsEffectSuite with PactForgerResources {

  @volatile private var testFailed: Boolean = false

  override def munitFixtures: Seq[Fixture[_]] = serverFixture +: additionalMunitFixtures

  def additionalMunitFixtures: Seq[Fixture[_]] = Seq.empty

  private val serverFixture: Fixture[BaseMockServer] = ResourceSuiteLocalFixture(
    "mockHttpServer",
    serverResource
  )

  private def serverResource: Resource[IO, BaseMockServer] =
    Resource.make[IO, BaseMockServer] {
      for {
        _ <- validatePactVersion.fold(IO.unit)(IO.raiseError)
        _ <- IO.delay(server.start())
        _ <- IO.delay(server.waitForServer())
      } yield server
    } { s =>
      if (testFailed) {
        logger.info(
          s"Not writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to file because tests failed."
        )
        IO.unit
      } else {
        logger.info(
          s"Writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}"
        )
        IO.delay(s.verifyResultAndWritePact(null, pactTestExecutionContext, pact, mockProviderConfig.getPactVersion))
      } >>
        IO.delay(s.stop())
    }

  def pactTest(name: String)(test: BaseMockServer => Any): Unit = this.test(name)(test(serverFixture.apply()))

  override def test(options: TestOptions)(body: => Any)(implicit loc: Location): Unit =
    munitTestsBuffer += munitTestTransform(
      new Test(
        options.name, { () =>
          try {
            PlatformCompat.waitAtMost(munitValueTransform(body), munitTimeout)
          } catch {
            case NonFatal(e) =>
              testFailed = true
              Future.failed(e)
          }
        },
        options.tags,
        loc
      )
    )
}
