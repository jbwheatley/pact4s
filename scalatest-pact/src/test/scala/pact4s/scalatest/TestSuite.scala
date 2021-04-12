package pact4s.scalatest

import au.com.dius.pact.consumer.{ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalaj.http.Http

class TestSuite extends AnyFlatSpec with Matchers with PactForger {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext("./scalatest-pact/target/pacts")

  def pact: RequestResponsePact = ConsumerPactBuilder
    .consumer("Consumer")
    .hasPactWith("Provider")
    .uponReceiving("a request to say Hello")
    .path("/hello")
    .method("POST")
    .body("{\"name\": \"harry\"}")
    .willRespondWith()
    .status(200)
    .body("{\"hello\": \"harry\"}")
    .uponReceiving("a request to say Goodbye")
    .path("/goodbye")
    .method("GET")
    .willRespondWith()
    .status(204)
    .toPact()

  it should "scalatest pact test" in {
    val result = Http(server.getUrl + "/hello").postData("{\"name\": \"harry\"}").header("content-type", "application/json").asString.body
    result shouldBe "{\"hello\": \"harry\"}"
  }

  it should "another scalatest pact test" in {
    val result = Http(server.getUrl + "/goodbye").header("content-type", "application/json").asString.code
    result shouldBe 204
  }
}
