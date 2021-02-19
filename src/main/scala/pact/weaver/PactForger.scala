package pact.weaver

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.{BaseMockServer, MockHttpServerKt, PactTestExecutionContext, PactVerificationResult}
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.support.V4PactFeaturesException
import cats.effect.{IO, Resource, Sync}
import cats.implicits._
import weaver._

import scala.jdk.CollectionConverters.ListHasAsScala

trait PactForger[F[_]] { self: MutableFSuite[F] { type Res = BaseMockServer } =>

  def pact: RequestResponsePact

  val mockProviderConfig: MockProviderConfig = MockProviderConfig.createDefault()

  private def validatePactVersion: Option[Throwable] = {
    val errors: List[String] = pact.validateForVersion(mockProviderConfig.getPactVersion).asScala.toList
    if (errors.isEmpty) None else {
      Some(new V4PactFeaturesException("Pact specification V4 features can not be used with version " + mockProviderConfig.getPactVersion.toString + " - " + errors.mkString(", ")))
    }

  }

  def pactTest(name: TestName): PactTestPartiallyApplied = new PactTestPartiallyApplied(name)

  class PactTestPartiallyApplied(name: TestName) {
    def apply(test: BaseMockServer => F[Expectations]) = {
      registerTest(name){ server =>
        Test(name.name, test(server)).flatTap { outcome =>
          Sync[F].suspend(
            Sync[F].catchNonFatal(server.verifyResultAndWritePact(outcome, new PactTestExecutionContext(), pact, mockProviderConfig.getPactVersion))
          )
        }
      }
    }
  }

  def server: Resource[F, BaseMockServer] = Resource.make[F, BaseMockServer] {
    val server: BaseMockServer = MockHttpServerKt.mockServer(pact, mockProviderConfig)
    for {
      _ <- Sync[F].delay(server.start())
      _ <- Sync[F].delay(server.waitForServer())
      _ <- validatePactVersion.fold(Sync[F].unit)(Sync[F].raiseError)
    } yield server
  }(s => Sync[F].delay(s.stop()))
}

trait IOPactForger extends PactForger[IO] with IOSuite {
  type Res = BaseMockServer
  override def sharedResource: Resource[IO, BaseMockServer] = this.server
}