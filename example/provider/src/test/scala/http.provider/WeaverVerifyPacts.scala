package http.provider

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.server.Server
import org.http4s.{BasicCredentials, HttpRoutes}
import pact4s.provider.ProviderRequestFilter.{NoOpFilter, SetHeaders}
import pact4s.provider.StateManagement.StateManagementFunction
import pact4s.provider._
import pact4s.weaver.PactVerifier
import weaver.IOSuite

import java.io.File
import scala.concurrent.duration.DurationInt

object WeaverVerifyPacts extends IOSuite with PactVerifier[IO] {

  type Res = Server

  override def sharedResource: cats.effect.Resource[IO, Server] = EmberServerBuilder
    .default[IO]
    .withHost(Host.fromString("localhost").get)
    .withPort(Port.fromInt(1235).get)
    .withHttpApp((fetchRoute <+> createRoute).orNotFound)
    .withShutdownTimeout(0.seconds)
    .build

  // Insert deliberately data that the provider state before hook should clean so that tests succeed
  val store: MockResourceStore[IO] = MockResourceStore.unsafe[IO](Map("missingId" -> 99, "newId" -> 66))

  val apiKey: String = "1dcbkjabyge1g273ie1u2"

  val fetchRoute: HttpRoutes[IO] = FetchRoute[IO](store.fetch, apiKey)

  val createRoute: HttpRoutes[IO] = CreateRoute[IO](store.create, apiKey)

  // If the auth header in the request is "correct", we can replace it with an auth header that will actually work with our API,
  // else we leave it as is to be rejected.
  def requestFilter: ProviderRequest => ProviderRequestFilter = req =>
    req.getFirstHeader("Authorization") match {
      case Some((_, value)) =>
        Authorization
          .parse(value)
          .map {
            case Authorization(BasicCredentials("user", "pass")) =>
              SetHeaders("Authorization" -> s"Basic ${BasicCredentials("user", apiKey).token}")
            case _ => NoOpFilter
          }
          .getOrElse(NoOpFilter)
      case None => NoOpFilter
    }

  val provider: ProviderInfoBuilder = ProviderInfoBuilder(
    "weaver-provider",
    PactSource.FileSource(
      Map("weaver-consumer" -> new File("../resources/pacts/weaver-consumer-weaver-provider.json"))
    )
  ).withHost("localhost")
    .withPort(1235)
    .withStateManagementFunction(
      StateManagementFunction {
        case ProviderState("resource exists", params) =>
          val id    = params.get("id")
          val value = params.get("value").map(_.toInt)
          (id, value).mapN(Resource.apply).traverse_(store.create).unsafeRunSync()
        case ProviderState("resource does not exist", _) => () // Nothing to do
        case _: ProviderState                            => ()
      }
        .withBeforeEach(() => store.empty.unsafeRunSync())
    )
    .withRequestFiltering(requestFilter)

  test("Verify pacts") {
    verifyPacts(
      publishVerificationResults = None,
      providerVerificationOptions = Nil,
      verificationTimeout = Some(10.seconds)
    ).map(succeed)
  }
}
