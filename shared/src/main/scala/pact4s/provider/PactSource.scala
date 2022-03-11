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

import java.io.File
import java.time.{Instant, LocalDate, OffsetDateTime}
import scala.concurrent.duration.FiniteDuration

sealed trait PactSource

object PactSource {

  /** It isn't necessary to use a pact broker to manage consumer pacts (though it is strongly recommended). The pacts
    * can also be directly loaded from files by using this pactSource as [[ProviderInfoBuilder.pactSource]]
    *
    * @param consumers
    *   A map of consumer names to the file where the pacts are written.
    */
  final class FileSource private (val consumers: Map[String, File]) extends PactSource

  object FileSource {
    def apply(consumer: Map[String, File]): FileSource           = new FileSource(consumer)
    def apply(consumer: (String, File), others: (String, File)*) = new FileSource(others.toMap + consumer)
  }

  sealed trait PactBroker extends PactSource {
    def brokerUrl: String
    def auth: Option[Authentication]
    def insecureTLS: Boolean
  }

  /** @param brokerUrl
    *   pact broker address
    * @param auth
    *   authentication for accessing the pact broker. Can be token or basic auth.
    * @param tags
    *   fetches all the latest pacts from the pact-broker for the provider with the given tags, all ignoring tags if
    *   [[tags]] is empty.
    */
  final class PactBrokerWithTags private (
      val brokerUrl: String,
      val insecureTLS: Boolean,
      val auth: Option[Authentication],
      val tags: List[String]
  ) extends PactBroker {
    private def copy(
        brokerUrl: String = brokerUrl,
        insecureTLS: Boolean = false,
        auth: Option[Authentication] = None,
        tags: List[String] = Nil
    ) = new PactBrokerWithTags(brokerUrl, insecureTLS, auth, tags)

    def withAuth(auth: Authentication): PactBrokerWithTags        = copy(auth = Some(auth))
    def withoutAuth: PactBrokerWithTags                           = copy(auth = None)
    def withTags(tags: List[String]): PactBrokerWithTags          = copy(tags = tags)
    def withTags(tags: String*): PactBrokerWithTags               = copy(tags = tags.toList)
    def withInsecureTLS(insecureTLS: Boolean): PactBrokerWithTags = copy(insecureTLS = insecureTLS)

    private[pact4s] def toPactBrokerWithSelectors: PactBrokerWithSelectors = PactBrokerWithSelectors(
      brokerUrl
    )
      .withOptionalAuth(auth)
      .withSelectors(tags.map(tag => ConsumerVersionSelector().withTag(tag)))
      .withInsecureTLS(insecureTLS)
  }

  object PactBrokerWithTags {
    @deprecated("Use PactBrokerWithSelectors instead.", "0.3.0")
    def apply(brokerUrl: String): PactBrokerWithTags =
      new PactBrokerWithTags(brokerUrl, insecureTLS = false, auth = None, tags = Nil)
  }

  /** @param brokerUrl
    *   pact broker address
    * @param auth
    *   authentication for accessing the pact broker. Can be token or basic auth.
    *
    * @see
    *   https://docs.pact.io/pact_broker/advanced_topics/pending_pacts for information on pending and WIP pacts
    *
    * @param enablePending
    *   enable pending pacts. Off by default. If enabled, [[providerTags]] must be provided.
    * @see
    *   also the master issue for pending pacts https://github.com/pact-foundation/pact_broker/issues/320
    *
    * @param includeWipPactsSince
    *   is a [[WipPactsSince]] which wraps an [[Option[java.time.Instant]]]. [[WipPactsSince]] also has constructors for
    *   using [[java.time.LocalDate]] and [[java.time.OffsetDateTime]] for convenience. All WIP pacts are pending pacts,
    *   so we enforce the setting of [[enablePending]] if this field is set.
    * @see
    *   also the master issue for WIP pacts here for more discussion
    *   https://github.com/pact-foundation/pact_broker/issues/338
    *
    * @param providerTags
    *   any tags that are going to be applied to the provider version when the verification results are published. Only
    *   used in the pending pact calculation, so will get set to empty when passed to pact-jvm if [[enablePending]] is
    *   set to false.
    *
    * @param selectors
    *   specifies which consumer pacts should be chosen for verification
    *
    * Example:
    * {{{
    *   PactBrokerWithSelectors(
    *     brokerUrl = "https://test.pact.dius.com.au"
    *   ).withPendingPactsEnabled(ProviderTags("MAIN"))
    *     .withAuth(BasicAuth("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1"))
    *     .withWipPactsSince(WipPactsSince.instant(Instant.EPOCH))
    *     .withSelectors(ConsumerVersionSelector())
    * }}}
    */
  final class PactBrokerWithSelectors private (
      val brokerUrl: String,
      val insecureTLS: Boolean,
      val auth: Option[Authentication],
      val enablePending: Boolean,
      val includeWipPactsSince: WipPactsSince,
      val providerTags: Option[ProviderTags],
      val selectors: List[ConsumerVersionSelector]
  ) extends PactBroker {
    private def copy(
        brokerUrl: String = brokerUrl,
        insecureTLS: Boolean = insecureTLS,
        auth: Option[Authentication] = auth,
        enablePending: Boolean = enablePending,
        includeWipPactsSince: WipPactsSince = includeWipPactsSince,
        providerTags: Option[ProviderTags] = providerTags,
        selectors: List[ConsumerVersionSelector] = selectors
    ): PactBrokerWithSelectors =
      new PactBrokerWithSelectors(
        brokerUrl,
        insecureTLS,
        auth,
        enablePending,
        includeWipPactsSince,
        providerTags,
        selectors
      )

    private[pact4s] def withOptionalAuth(auth: Option[Authentication]): PactBrokerWithSelectors = copy(auth = auth)

    def withAuth(auth: Authentication): PactBrokerWithSelectors = withOptionalAuth(Some(auth))

    def withPendingPactsEnabled(providerTags: ProviderTags): PactBrokerWithSelectors =
      copy(enablePending = true, providerTags = Some(providerTags))

    def withPendingPactsDisabled: PactBrokerWithSelectors =
      copy(enablePending = false, includeWipPactsSince = WipPactsSince.never)

    def withPendingPacts(enabled: Boolean): PactBrokerWithSelectors =
      copy(enablePending = enabled, includeWipPactsSince = if (enabled) includeWipPactsSince else WipPactsSince.never)

    @deprecated(message = "Use withWipPactsSince(since: WipPactsSince)", since = "0.0.19")
    def withWipPactsSince(since: Instant, providerTags: ProviderTags): PactBrokerWithSelectors =
      withWipPactsSince(WipPactsSince.instant(since)).withProviderTags(providerTags)

    @deprecated(message = "Use withWipPactsSince(since: WipPactsSince)", since = "0.0.19")
    def withWipPactsSince(since: LocalDate, providerTags: ProviderTags): PactBrokerWithSelectors =
      withWipPactsSince(WipPactsSince.localDate(since)).withProviderTags(providerTags)

    @deprecated(message = "Use withWipPactsSince(since: WipPactsSince)", since = "0.0.19")
    def withWipPactsSince(since: OffsetDateTime, providerTags: ProviderTags): PactBrokerWithSelectors =
      withWipPactsSince(WipPactsSince.offsetDateTime(since)).withProviderTags(providerTags)

    /** this method is somewhat unsafe, as it is illegal to enable pending pacts (of which WIP pacts are a subset)
      * without provider tags being provided.
      */
    def withWipPactsSince(since: WipPactsSince): PactBrokerWithSelectors =
      copy(enablePending = true, includeWipPactsSince = since)

    def withProviderTags(providerTags: ProviderTags): PactBrokerWithSelectors = copy(providerTags = Some(providerTags))

    def withOptionalProviderTags(providerTags: Option[ProviderTags]): PactBrokerWithSelectors =
      copy(providerTags = providerTags)

    def withSelectors(selectors: List[ConsumerVersionSelector]): PactBrokerWithSelectors =
      copy(selectors = selectors)

    def withSelectors(selectors: ConsumerVersionSelector*): PactBrokerWithSelectors =
      copy(selectors = selectors.toList)

    def withInsecureTLS(insecureTLS: Boolean): PactBrokerWithSelectors = copy(insecureTLS = insecureTLS)

    private[pact4s] def validate(): Unit = {
      require(!(enablePending && providerTags.isEmpty), "Provider tags must be provided if pending pacts are enabled")
      require(
        !(includeWipPactsSince.since.isDefined && providerTags.isEmpty),
        "Provider tags must be provided if WIP pacts are enabled"
      )
    }
  }

  object PactBrokerWithSelectors {
    def apply(
        brokerUrl: String
    ): PactBrokerWithSelectors =
      new PactBrokerWithSelectors(
        brokerUrl = brokerUrl,
        insecureTLS = false,
        auth = None,
        enablePending = false,
        includeWipPactsSince = WipPactsSince.never,
        providerTags = None,
        selectors = List(ConsumerVersionSelector())
      )

    @deprecated(message = "Use the other apply method with the safer builder patterns", since = "0.0.17")
    def apply(
        brokerUrl: String,
        auth: Option[Authentication] = None,
        enablePending: Boolean = false,
        includeWipPactsSince: Option[FiniteDuration] = None,
        providerTags: List[String] = Nil,
        selectors: List[ConsumerVersionSelector] = List(ConsumerVersionSelector())
    ): PactBrokerWithSelectors = {
      val tags = ProviderTags.fromList(providerTags)
      new PactBrokerWithSelectors(
        brokerUrl = brokerUrl,
        insecureTLS = false,
        auth = auth,
        enablePending = enablePending,
        includeWipPactsSince = includeWipPactsSince
          .map(d => WipPactsSince.instant(Instant.ofEpochSecond(d.toSeconds)))
          .getOrElse(WipPactsSince.never),
        providerTags = tags,
        selectors = selectors
      )
    }
  }
}
