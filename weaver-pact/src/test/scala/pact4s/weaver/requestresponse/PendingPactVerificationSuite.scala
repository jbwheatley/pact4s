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

package pact4s.weaver.requestresponse

import cats.effect.IO
import pact4s.PendingPactVerificationFixture
import pact4s.weaver.PactVerifier
import weaver.SimpleIOSuite

object PendingPactVerificationSuite
    extends SimpleIOSuite
    with PactVerifier[IO]
    with PendingPactVerificationFixture[IO] {
  test("pending pact failure should be skipped")(verifyPacts().map(succeed))
}
