package pact4s.weaver

import au.com.dius.pact.consumer.{ConsumerPactBuilder, PactTestExecutionContext}
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.IO
import scalaj.http.Http
import weaver.IOSuite

object TestSuite extends IOSuite with PactForger[IO] {
  override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext("./weaver-pact/target/pacts")

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

  test("weaver pact test") { server =>
    IO(Http(server.getUrl + "/hello").postData("{\"name\": \"harry\"}").header("content-type", "application/json").asString.body)
      .map(r => expect(r == "{\"hello\": \"harry\"}"))
  }

  test("another weaver pact test") { server =>
    IO(Http(server.getUrl + "/goodbye").header("content-type", "application/json").asString.code)
      .map(r => expect(r == 204))
  }
}
