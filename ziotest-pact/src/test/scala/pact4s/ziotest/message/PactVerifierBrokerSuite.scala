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

package pact4s.ziotest.message

import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}
import pact4s.ziotest.MessagePactVerifier
import zio.{Scope, ZIO}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object PactVerifierBrokerSuite extends ZIOSpecDefault with MessagePactVerifier {
  lazy val mock = new MockProviderServer(49156)

  def messages: ResponseFactory     = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.brokerMessageProviderInfo
  override def spec: Spec[TestEnvironment with Scope, Any] =
    test("Verify pacts for provider `MessageProvider`, zio-test")(
      ZIO
        .attempt(
          verifyPacts(publishVerificationResults = Some(PublishVerificationResults(providerVersion = "SNAPSHOT")))
        )
        .as(assertTrue(true))
    )
}
