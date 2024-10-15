/*
 * Copyright 2021 io.github.jbwheatley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    .`given`("bob exists", Map[String, Any]("person" -> "bob", "a" -> 1, "b" -> List("str")))
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
    .uponReceiving("a request with auth header")
    .path("/authorized")
    .method("GET")
    .headerFromProviderState("Authorization", "Bearer ${bearerToken}", "Bearer super-secure")
    .willRespondWith()
    .status(200)
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
