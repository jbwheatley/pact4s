package munit.pact

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.IO
import scalaj.http.Http

class TestSuite extends PactForger {
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

  pactTest("munit.pact test") { server =>
    IO(Http(server.getUrl + "/hello").postData("{\"name\": \"harry\"}").header("content-type", "application/json").asString.body)
      .assertEquals("{\"hello\": \"harry\"}")
  }

  pactTest("another munit.pact test") { server =>
    IO(Http(server.getUrl + "/goodbye").header("content-type", "application/json").asString.code)
      .assertEquals(205)
  }
}
