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

package pact4s.weaver.message

import cats.effect.IO
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.{ProviderInfoBuilder, PublishVerificationResults}
import pact4s.weaver.MessagePactVerifier
import weaver.SimpleIOSuite

object PactVerifierBrokerSuite extends SimpleIOSuite with MessagePactVerifier[IO] {
  val mock = new MockProviderServer(49161)

  def messages: ResponseFactory              = MessagesProvider.messages
  override val provider: ProviderInfoBuilder = mock.brokerMessageProviderInfo

  pureTest("Verify pacts for provider `MessageProvider`") {
    succeed(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT"
          )
        )
      )
    )
  }
}
