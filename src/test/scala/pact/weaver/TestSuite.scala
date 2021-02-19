package pact.weaver
import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.RequestResponsePact
import cats.effect.IO
import scalaj.http.Http

object TestSuite extends IOPactForger {
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
    .toPact()

  pactTest("pact test") { server =>
    IO(Http(server.getUrl + "/hello").postData("{\"name\": \"harry\"}").header("content-type", "application/json").asString).map {
      _ => expect(true)
    }

  }
}
