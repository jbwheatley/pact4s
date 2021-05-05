package pact4s.munit

import au.com.dius.pact.core.model.messaging.Message
import cats.effect.{IO, Resource, SyncIO}
import munit.internal.PlatformCompat
import munit.{CatsEffectSuite, Location, TestOptions}
import pact4s.MessagePactForgerResources

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.control.NonFatal

trait MessagePactForger extends CatsEffectSuite with MessagePactForgerResources {

  @volatile private var testFailed = false

  val messages: SyncIO[FunFixture[List[Message]]] = ResourceFixture(
    Resource.pure[IO, List[Message]](pact.getMessages.asScala.toList)
  )

  override def test(options: TestOptions)(body: => Any)(implicit loc: Location): Unit =
    munitTestsBuffer += munitTestTransform(
      new Test(
        options.name,
        () =>
          try PlatformCompat.waitAtMost(munitValueTransform(body), munitTimeout)
          catch {
            case NonFatal(e) =>
              testFailed = true
              Future.failed(e)
          },
        options.tags,
        loc
      )
    )

  override def beforeAll(): Unit = {
    super.beforeAll()
    validatePactVersion.foreach[Unit](throw _)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    if (testFailed) {
      logger.info(
        s"Not writing message pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to file because tests failed."
      )
    } else {
      logger.info(
        s"Writing message pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}"
      )
      val write = pact.write(pactTestExecutionContext.getPactFolder, pactSpecVersion)
      Option(write.component2()).foreach(throw _)
    }
  }
}
