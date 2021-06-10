/*
 * Copyright 2021-2021 io.github.jbwheatley
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

import au.com.dius.pact.provider.ProviderVerifier

sealed abstract class ProviderVerificationOption(val key: String) {
  def value: String
}

object ProviderVerificationOption {

  sealed abstract class BooleanProviderVerificationOption(_key: String) extends ProviderVerificationOption(_key) {
    def value: String = "true"
  }
  sealed abstract class RegexProviderVerificationOption(_key: String, regex: String)
      extends ProviderVerificationOption(_key) {
    def value: String = regex
  }
  sealed abstract class ListProviderVerificationOption(_key: String, values: List[String])
      extends ProviderVerificationOption(_key) {
    def value: String = values.mkString(",")
  }

  case object SHOW_STACKTRACE extends BooleanProviderVerificationOption(ProviderVerifier.PACT_SHOW_STACKTRACE)

  case object VERIFIER_PUBLISH_RESULTS
      extends BooleanProviderVerificationOption(ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS)

  final case class FILTER_CONSUMERS(consumers: List[String])
      extends ListProviderVerificationOption(ProviderVerifier.PACT_FILTER_CONSUMERS, consumers)

  final case class FILTER_DESCRIPTION(regex: String)
      extends RegexProviderVerificationOption(ProviderVerifier.PACT_FILTER_DESCRIPTION, regex)

  final case class FILTER_PROVIDERSTATE(regex: String)
      extends RegexProviderVerificationOption(ProviderVerifier.PACT_FILTER_PROVIDERSTATE, regex)

  case object SHOW_FULLDIFF extends BooleanProviderVerificationOption(ProviderVerifier.PACT_SHOW_FULLDIFF)

  case object PROVIDER_VERSION_TRIM_SNAPSHOT
      extends BooleanProviderVerificationOption(ProviderVerifier.PACT_PROVIDER_VERSION_TRIM_SNAPSHOT)
}
