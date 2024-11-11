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

package pact4s.scalatest.message.issue19

import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import pact4s.MockProviderServer
import pact4s.provider.ProviderInfoBuilder
import pact4s.provider.ResponseBuilder.MessageAndMetadataBuilder
import pact4s.provider.VerificationSettings.AnnotatedMethodVerificationSettings
import pact4s.scalatest.PactVerifier

import scala.jdk.CollectionConverters._

class ReproducerSuite extends AnyFlatSpec with PactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer(49160)

  // Because this is mutable, it will trigger the issue.
  var metadata: Map[String, String] = _

  def provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    consumerName = "Pact4sMessageConsumer",
    providerName = "Pact4sMessageProvider",
    fileName = "./scripts/Pact4sMessageConsumer-Pact4sMessageProvider.json",
    verificationSettings =
      Some(AnnotatedMethodVerificationSettings(packagesToScan = List("pact4s.scalatest.message.issue19"))),
    isHttpPact = false
  )

  @PactVerifyProvider("A message to say goodbye")
  def goodbyeMessage(): MessageAndMetadata = {
    val metadata = Map.empty[String, String]
    val body     = Json.obj("goodbye" -> "harry".asJson)
    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
  }

  @PactVerifyProvider("A message to say hello")
  def helloMessage(): MessageAndMetadata = {
    val body = Json.obj("hello" -> "harry".asJson)
    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
  }

  @PactVerifyProvider("A message with nested arrays in the body")
  def nestedArrayMessage(): MessageAndMetadata = {
    val body = """{"array": [1,2,3]}"""
    MessageAndMetadataBuilder(body).build
  }

  @PactVerifyProvider("A message with a json array as content")
  def topLevelArrayMessage(): MessageAndMetadata = {
    val body = """[{"a":1},{"b":true}]"""
    MessageAndMetadataBuilder(body).build
  }

  override def beforeAll(): Unit =
    metadata = Map("hi" -> "there")

  it should "verify pacts" in {
    verifyPacts(
      // Issue #19
      // If the declaring class of the annotated method has mutable properties, we must ensure the verifier uses the same
      // instance is used when invoking the method, otherwise the state of the property will be wrong.
      providerMethodInstance = Some(this)
    )
  }
}
