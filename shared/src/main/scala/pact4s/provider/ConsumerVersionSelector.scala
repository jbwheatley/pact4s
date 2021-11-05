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

import au.com.dius.pact.core.pactbroker.{ConsumerVersionSelector => PactJVMSelector}

/** @see
  *   https://docs.pact.io/pact_broker/advanced_topics/consumer_version_selectors/ and
  *   https://github.com/pact-foundation/pact_broker/issues/307 Gets converted into a
  *   au.com.dius.pact.core.pactbroker.ConsumerVersionSelector under the hood.
  *
  * @param tag
  *   the tag name(s) of the consumer versions to get the pacts for.
  * @param latest
  *   the name of the tag to fallback to if the specified tag does not exist. This is useful when the consumer and
  *   provider use matching branch names to coordinate the development of new features.
  * @param fallbackTag
  *   default true. If the latest flag is omitted, all the pacts with the specified tag will be returned. (This might
  *   seem a bit weird, but it's done this way to match the syntax used for the matrix query params. See
  *   https://docs.pact.io/selectors)
  * @param consumer
  *   allows a selector to only be applied to a certain consumer. This is used for example when there is an API that has
  *   multiple consumers, one of which is a deployed service, and one of which is a mobile consumer. The deployed
  *   service only needs the latest production pact verified, where as the mobile consumer may want all the production
  *   pacts verified.
  */
final case class ConsumerVersionSelector(
    tag: Option[String] = None,
    latest: Boolean = true,
    fallbackTag: Option[String] = None,
    consumer: Option[String] = None
) {
  def withTag(tag: String): ConsumerVersionSelector           = this.copy(tag = Some(tag))
  def withFallbackTag(tag: String): ConsumerVersionSelector   = this.copy(fallbackTag = Some(tag))
  def withConsumer(consumer: String): ConsumerVersionSelector = this.copy(consumer = Some(consumer))

  private[pact4s] def toPactJVMSelector: PactJVMSelector =
    new PactJVMSelector(tag.orNull, latest, consumer.orNull, fallbackTag.orNull)
}
