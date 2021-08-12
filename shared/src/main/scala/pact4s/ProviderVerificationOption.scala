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

  /** Gives a more detailed output for pact verification failures */
  case object SHOW_STACKTRACE extends BooleanProviderVerificationOption(ProviderVerifier.PACT_SHOW_STACKTRACE)

  /** Doesn't need to be set if publishVerificationResults is non-empty in [[PactVerifyResources.verifyPacts]] */
  case object VERIFIER_PUBLISH_RESULTS
      extends BooleanProviderVerificationOption(ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS)

  /** @param consumers pacts for these consumers won't be verified */
  final case class FILTER_CONSUMERS(consumers: List[String])
      extends ListProviderVerificationOption(ProviderVerifier.PACT_FILTER_CONSUMERS, consumers)

  /** @param regex won't verify pacts whose descriptions match this regex */
  final case class FILTER_DESCRIPTION(regex: String)
      extends RegexProviderVerificationOption(ProviderVerifier.PACT_FILTER_DESCRIPTION, regex)

  /** @param regex won't verify pacts whose states match this regex */
  final case class FILTER_PROVIDERSTATE(regex: String)
      extends RegexProviderVerificationOption(ProviderVerifier.PACT_FILTER_PROVIDERSTATE, regex)

  /** Gives a more detailed output for pact verification results */
  case object SHOW_FULLDIFF extends BooleanProviderVerificationOption(ProviderVerifier.PACT_SHOW_FULLDIFF)

  /** Will trim the snapshot off the provider version before publishing the results */
  case object PROVIDER_VERSION_TRIM_SNAPSHOT
      extends BooleanProviderVerificationOption(ProviderVerifier.PACT_PROVIDER_VERSION_TRIM_SNAPSHOT)

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
}
