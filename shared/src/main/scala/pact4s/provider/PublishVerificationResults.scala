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
package provider

final case class PublishVerificationResults(
    providerVersion: String,
    providerTags: Option[ProviderTags]
)

object PublishVerificationResults {
  @deprecated("use ProviderTags(..) or ProviderTags.fromList(..) rather than List[String]", "0.0.19")
  def apply(providerVersion: String, providerTags: List[String]): PublishVerificationResults =
    PublishVerificationResults(providerVersion, ProviderTags.fromList(providerTags))

  def apply(providerVersion: String): PublishVerificationResults = PublishVerificationResults(providerVersion, None)

  def apply(providerVersion: String, providerTags: ProviderTags): PublishVerificationResults =
    PublishVerificationResults(providerVersion, Some(providerTags))
}
