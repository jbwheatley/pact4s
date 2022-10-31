package pact4s

import au.com.dius.pact.consumer.ConsumerPactBuilder
import io.circe.Json
import io.circe.syntax.EncoderOps
import pact4s.circe.implicits._
import pact4s.syntax.{MessagePactOps, RequestResponsePactOps}

object TestModels extends RequestResponsePactOps with MessagePactOps {

  val requestResponsePact = ConsumerPactBuilder
    .consumer("Pact4sConsumer")
    .hasPactWith("Pact4sProvider")
    .uponReceiving("a request to say Hello")
    .path("/hello")
    .method("POST")
    .body(Json.obj("name" -> "harry".asJson), "application/json")
    .headers("other-header" -> "howdy")
    .willRespondWith()
    .status(200)
    .body(Json.obj("hello" -> "harry".asJson))
    .uponReceiving("a request to say Goodbye")
    .path("/goodbye")
    .method("GET")
    .willRespondWith()
    .status(204)
    .`given`("bob exists", Map("person" -> "bob"))
    .uponReceiving("a request to find a friend")
    .path("/anyone-there/bob")
    .method("GET")
    .willRespondWith()
    .status(200)
    .body(Json.obj("found" -> "bob".asJson))
    .`given`("nobody is home")
    .uponReceiving("a request to find anyone")
    .path("/anyone-there")
    .willRespondWith()
    .status(404)
    .toPact()

  val messagePact = Pact4sMessagePactBuilder()
    .consumer("Pact4sMessageConsumer")
    .hasPactWith("Pact4sMessageProvider")
    .expectsToReceive("A message to say hello")
    .withContent(Json.obj("hello" -> "harry".asJson))
    .withMetadata(Map("hi" -> "there"))
    .expectsToReceive("A message to say goodbye")
    .withContent(Json.obj("goodbye" -> "harry".asJson))
    .expectsToReceive("A message with nested arrays in the body")
    .withContent(Json.obj("array" -> List(1, 2, 3).asJson))
    .expectsToReceive("A message with a json array as content")
    .withContent(Json.arr(Json.obj("a" -> 1.asJson), Json.obj("b" -> true.asJson)))
    .toMessagePact
}
