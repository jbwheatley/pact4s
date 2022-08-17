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
package provider

import au.com.dius.pact.core.pactbroker.{ConsumerVersionSelector => PactJVMSelector}
import au.com.dius.pact.core.support.json.JsonValue

/** @see
  *   https://docs.pact.io/pact_broker/advanced_topics/consumer_version_selectors/ and
  *   https://github.com/pact-foundation/pact_broker/issues/307. Gets converted into a
  *   au.com.dius.pact.core.pactbroker.ConsumerVersionSelector under the hood.
  *
  * @see
  *   https://github.com/pact-foundation/pact_broker/blob/master/lib/pact_broker/doc/views/provider-pacts-for-verification.markdown
  *   for a list of supported selectors.
  *
  * @param tag
  *   the tag name(s) of the consumer versions to get the pacts for. This field is still supported but it is recommended
  *   to use the branch in preference now.
  * @param latest
  *   Used in conjunction with the tag property. If a tag is specified, and latest is true, then the latest pact for
  *   each of the consumers with that tag will be returned. If a tag is specified and the latest flag is not set to
  *   true, all the pacts with the specified tag will be returned. (This might seem a bit weird, but it's done this way
  *   to match the syntax used for the matrix query params. See https://docs.pact.io/selectors).
  * @param fallbackTag
  *   the name of the tag to fallback to if the specified tag does not exist. This is useful when the consumer and
  *   provider use matching branch names to coordinate the development of new features. This field is still supported
  *   but it is recommended to use the fallbackBranch in preference now.
  * @param consumer
  *   allows a selector to only be applied to a certain consumer. This is used for example when there is an API that has
  *   multiple consumers, one of which is a deployed service, and one of which is a mobile consumer. The deployed
  *   service only needs the latest production pact verified, where as the mobile consumer may want all the production
  *   pacts verified.
  * @param branch
  *   the branch name of the consumer versions to get the pacts for. Use of this selector requires that the consumer has
  *   configured a branch name when publishing the pacts.
  * @param mainBranch
  *   Return the pacts for the configured mainBranch of each consumer.Use of this selector requires that the consumer
  *   has configured the mainBranch property, and has set a branch name when publishing the pacts.
  * @param matchingBranch
  *   When true, returns the latest pact for any branch with the same name as the specified providerVersionBranch.
  * @param additionalSelectors
  *   see
  *   https://github.com/pact-foundation/pact_broker/blob/master/lib/pact_broker/doc/views/provider-pacts-for-verification.markdown
  *   for a list of available selectors that can be set. This list is subject to change and so we leave it to the user
  *   add any other selectors they require here rather than having them as strongly-typed fields.
  */
@deprecated(
  "Use pact4s.provider.ConsumerVersionSelectors instead, which closely models the new underlying java impl",
  "0.5.0"
)
final class ConsumerVersionSelector private (
    val tag: Option[String],
    val latest: Boolean,
    val fallbackTag: Option[String],
    val consumer: Option[String],
    val branch: Option[String],
    val mainBranch: Option[Boolean],
    val matchingBranch: Option[Boolean],
    val additionalSelectors: Map[String, Any]
) {
  private def copy(
      tag: Option[String] = tag,
      latest: Boolean = latest,
      fallbackTag: Option[String] = fallbackTag,
      consumer: Option[String] = consumer,
      branch: Option[String] = branch,
      mainBranch: Option[Boolean] = mainBranch,
      matchingBranch: Option[Boolean] = matchingBranch,
      additionalSelectors: Map[String, Any] = additionalSelectors
  ) = new ConsumerVersionSelector(
    tag,
    latest,
    fallbackTag,
    consumer,
    branch,
    mainBranch,
    matchingBranch,
    additionalSelectors
  )

  def withTag(tag: String): ConsumerVersionSelector = copy(tag = Some(tag))

  def withFallbackTag(tag: String): ConsumerVersionSelector = copy(fallbackTag = Some(tag))

  def withConsumer(consumer: String): ConsumerVersionSelector = copy(consumer = Some(consumer))

  def withBranch(branch: String): ConsumerVersionSelector = copy(branch = Some(branch))

  @deprecated("Use withMainBranch without the argument, as if this is set it can only be true.", "0.3.0")
  def withMainBranch(mainBranch: Boolean): ConsumerVersionSelector = copy(mainBranch = Some(mainBranch))

  def withMainBranch: ConsumerVersionSelector = copy(mainBranch = Some(true))

  @deprecated("Use withMatchingBranch without the argument, as if this is set it can only be true.", "0.3.0")
  def withMatchingBranch(matchingBranch: Boolean): ConsumerVersionSelector =
    copy(matchingBranch = Some(matchingBranch))

  def withMatchingBranch: ConsumerVersionSelector = copy(matchingBranch = Some(true))

  def withAdditionalSelectors(selectors: (String, Any)*): ConsumerVersionSelector =
    copy(additionalSelectors = selectors.toMap)

  /*
  May need improvement if selectors with array values are allowed, but at the time of writing only Boolean and String are expected.
   */
  private[provider] def toJson: JsonValue = {
    val json = new PactJVMSelector(tag.orNull, latest, consumer.orNull, fallbackTag.orNull).toJson.asObject()
    branch.foreach(b => json.add("branch", new JsonValue.StringValue(b)))
    mainBranch.foreach(_ => json.add("main_branch", JsonValue.True.INSTANCE))
    matchingBranch.foreach(_ => json.add("matching_branch", JsonValue.True.INSTANCE))
    additionalSelectors.foreach { case (k, v) =>
      v match {
        case bool: Boolean =>
          json.add(k, if (bool) JsonValue.True.INSTANCE else JsonValue.False.INSTANCE)
        case _ =>
          json.add(k, new JsonValue.StringValue(v.toString))
      }
    }
    json
  }
}

object ConsumerVersionSelector {

  /** Entry point for constructing a [[ConsumerVersionSelector]]. Without any other arguments set, this will simply
    * fetch all the latest pacts that have been forged against the provider. Greater selector specificity can be added
    * by using the builder methods on ConsumerVersionSelector, e.g.
    *
    * {{{
    *   ConsumerVersionSelector().withTag("tag").withBranch("main").withConsumer("consumer1")
    * }}}
    *
    * etc.
    */
  @deprecated(
    "Use pact4s.provider.ConsumerVersionSelectors instead, which closely models the new underlying java impl",
    "0.5.0"
  )
  def apply(): ConsumerVersionSelector = new ConsumerVersionSelector(
    tag = None,
    latest = true,
    fallbackTag = None,
    consumer = None,
    branch = None,
    mainBranch = None,
    matchingBranch = None,
    additionalSelectors = Map.empty
  )
}
