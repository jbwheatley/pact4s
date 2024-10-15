package http.consumer

import au.com.dius.pact.consumer.{ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.IO
import cats.effect.unsafe.implicits._
import io.circe.Json
import io.circe.syntax._
import org.http4s.{BasicCredentials, Uri}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.circe.implicits._
import pact4s.scalatest.RequestResponsePactForger

class ScalaTestPact extends AnyFlatSpec with Matchers with ExamplePactCommons with RequestResponsePactForger {

  override val pactTestExecutionContext: PactTestExecutionContext = executionContext

  val pact: RequestResponsePact =
    ConsumerPactBuilder
      .consumer("scalatest-consumer")
      .hasPactWith("scalatest-provider")
      // -------------------------- FETCH RESOURCE --------------------------
      .`given`(
        "resource exists", // this is a state identifier that is passed to the provider
        Map[String, Any](
          "id"    -> testID,
          "value" -> 123
        ) // we can use parameters to specify details about the provider state
      )
      .uponReceiving("Request to fetch extant resource")
      .method("GET")
      .path(s"/resource/$testID")
      .headers("Authorization" -> mkAuthHeader("pass"))
      .willRespondWith()
      .status(200)
      .body(
        Json.obj("id" -> testID.asJson, "value" -> 123.asJson)
      ) // can use circe json directly for both request and response bodies with `import pact4s.circe.implicits._`
      .`given`("resource does not exist")
      .uponReceiving("Request to fetch missing resource")
      .method("GET")
      .path(s"/resource/$missingID")
      .headers("Authorization" -> mkAuthHeader("pass"))
      .willRespondWith()
      .status(404)
      .uponReceiving("Request to fetch resource with wrong auth")
      .method("GET")
      .path(s"/resource/$testID")
      .headers("Authorization" -> mkAuthHeader("wrong"))
      .willRespondWith()
      .status(401)
      // -------------------------- CREATE RESOURCE --------------------------
      .`given`("resource does not exist")
      .uponReceiving("Request to create new resource")
      .method("POST")
      .path("/resource")
      .headers("Authorization" -> mkAuthHeader("pass"))
      .body(newResource) // can use classes directly in the body if they are encodable
      .willRespondWith()
      .status(204)
      .`given`(
        "resource exists",
        Map[String, Any]("id" -> conflictResource.id, "value" -> conflictResource.value)
      ) // notice we're using the same state, but with different parameters
      .uponReceiving("Request to create resource that already exists")
      .method("POST")
      .path("/resource")
      .headers("Authorization" -> mkAuthHeader("pass"))
      .body(conflictResource)
      .willRespondWith()
      .status(409)
      .toPact

  /*
  we should use these tests to ensure that our client class correctly handles responses from the provider - i.e. decoding, error mapping, validation
   */
  it should "handle fetch request for extant resource" in {
    new ProviderClientImpl[IO](client, Uri.unsafeFromString(mockServer.getUrl), BasicCredentials("user", "pass"))
      .fetchResource(testID)
      .unsafeRunSync() shouldBe Some(Resource(testID, 123))
  }

  it should "handle fetch request for missing resource" in {
    new ProviderClientImpl[IO](client, Uri.unsafeFromString(mockServer.getUrl), BasicCredentials("user", "pass"))
      .fetchResource(missingID)
      .unsafeRunSync() shouldBe None
  }

  it should "handle fetch request with incorrect auth" in {
    new ProviderClientImpl[IO](client, Uri.unsafeFromString(mockServer.getUrl), BasicCredentials("user", "wrong"))
      .fetchResource(testID)
      .attempt
      .unsafeRunSync() shouldBe Left(InvalidCredentials)
  }

  it should "handle create request for new resource" in {
    new ProviderClientImpl[IO](client, Uri.unsafeFromString(mockServer.getUrl), BasicCredentials("user", "pass"))
      .createResource(newResource)
      .unsafeRunSync()
  }

  it should "handle create request for existing resource" in {
    new ProviderClientImpl[IO](client, Uri.unsafeFromString(mockServer.getUrl), BasicCredentials("user", "pass"))
      .createResource(conflictResource)
      .attempt
      .unsafeRunSync() shouldBe Left(UserAlreadyExists)
  }
}
