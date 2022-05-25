package http.provider

import cats.effect.IO
import cats.implicits._
import com.comcast.ip4s.IpLiteralSyntax
import munit.CatsEffectSuite
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.server.Server
import org.http4s.{BasicCredentials, HttpRoutes}
import pact4s.munit.PactVerifier
import pact4s.provider.ProviderRequestFilter.{NoOpFilter, SetHeaders}
import pact4s.provider._

import java.io.File
import scala.concurrent.duration.DurationInt

class MunitVerifyPacts extends CatsEffectSuite with PactVerifier {

  val store: MockResourceStore[IO] = MockResourceStore.unsafe[IO]

  val apiKey: String = "1dcbkjabyge1g273ie1u2"

  val fetchRoute: HttpRoutes[IO] = FetchRoute[IO](store.fetch, apiKey)

  val createRoute: HttpRoutes[IO] = CreateRoute[IO](store.create, apiKey)

  val server: Fixture[Server] = ResourceSuiteLocalFixture(
    "provider-server",
    EmberServerBuilder
      .default[IO]
      .withHost(host"localhost")
      .withPort(port"1234")
      .withHttpApp((fetchRoute <+> createRoute).orNotFound)
      .build
  )

  override def munitFixtures: Seq[Fixture[_]] = Seq(server)

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
    "munit-provider",
    PactSource.FileSource(
      Map("munit-consumer" -> new File("./example/resources/pacts/munit-consumer-munit-provider.json"))
    )
  ).withHost("localhost")
    .withPort(1234)
    .withStateChangeFunction {
      case ProviderState("resource exists", params) =>
        val id    = params.get("id")
        val value = params.get("value").map(_.toInt)
        (id, value).mapN(Resource.apply).traverse_(store.create).unsafeRunSync()
      case ProviderState("resource does not exists", _) => store.empty.unsafeRunSync()
    }
    .withRequestFiltering(requestFilter)

  test("Verify pacts") {
    verifyPacts(
      publishVerificationResults = None,
      providerVerificationOptions = Nil,
      verificationTimeout = Some(10.seconds)
    )
  }
}
