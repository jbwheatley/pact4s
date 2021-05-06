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

import au.com.dius.pact.core.model.{FileSource => PactJVMFileSource}
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.core.pactbroker.{ConsumerVersionSelector => PactJVMSelector}
import pact4s.Authentication.{BasicAuth, TokenAuth}
import pact4s.PactSource.{FileSource, PactBroker, PactBrokerWithSelectors, PactBrokerWithTags}

import java.io.File
import java.util.Date
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.SeqHasAsJava

final case class ProviderInfoBuilder(
    name: String,
    protocol: String,
    host: String,
    port: Int,
    path: String,
    pactSource: PactSource
) {
  private[pact4s] def toProviderInfo: ProviderInfo = {
    val p = new ProviderInfo(name, protocol, host, port, path)
    pactSource match {
      case broker: PactBroker => applyBrokerSourceToProvider(p, broker)
      case FileSource(consumer, file) =>
        p.hasPactWith(
          consumer,
          { consumer =>
            consumer.setPactSource(new PactJVMFileSource(file))
            kotlin.Unit.INSTANCE
          }
        )
        p
    }
  }

  @tailrec
  private def applyBrokerSourceToProvider(
      providerInfo: ProviderInfo,
      pactSource: PactBroker
  ): ProviderInfo =
    pactSource match {
      case PactBrokerWithSelectors(brokerUrl, auth, enablePending, includeWipPactsSince, providerTags, selectors) =>
        val options: java.util.HashMap[String, Any] = new java.util.HashMap()
        options.put("enablePending", enablePending)
        options.put("providerTags", providerTags.asJava)
        auth.foreach {
          case TokenAuth(token)      => options.put("authentication", List("bearer", token).asJava)
          case BasicAuth(user, pass) => options.put("authentication", List("basic", user, pass).asJava)
        }
        includeWipPactsSince.foreach(since => options.put("includeWipPactsSince", new Date(since.toMillis)))
        providerInfo.hasPactsFromPactBrokerWithSelectors(options, brokerUrl, selectors.map(_.toPactJVMSelector).asJava)
        providerInfo
      case PactBrokerWithTags(brokerUrl, auth, tags) =>
        applyBrokerSourceToProvider(
          providerInfo,
          PactBrokerWithSelectors(
            brokerUrl,
            auth,
            enablePending = false,
            None,
            Nil,
            tags.map(tag => ConsumerVersionSelector(Some(tag)))
          )
        )
    }
}

sealed trait PactSource

object PactSource {
  final case class FileSource(consumer: String, file: File) extends PactSource

  sealed trait PactBroker extends PactSource {
    def brokerUrl: String
    def auth: Option[Authentication]
  }
  final case class PactBrokerWithTags(brokerUrl: String, auth: Option[Authentication] = None, tags: List[String] = Nil)
      extends PactBroker
  final case class PactBrokerWithSelectors(
      brokerUrl: String,
      auth: Option[Authentication] = None,
      enablePending: Boolean = false,
      includeWipPactsSince: Option[FiniteDuration] = None,
      providerTags: List[String] = Nil,
      selectors: List[ConsumerVersionSelector] = List(ConsumerVersionSelector())
  ) extends PactBroker
}

/*
consumerVersionSelectors.tag: the tag name(s) of the consumer versions to get the pacts for.

consumerVersionSelectors.fallbackTag: the name of the tag to fallback to if the specified tag does not exist.
This is useful when the consumer and provider use matching branch names to coordinate the development of new features.

consumerVersionSelectors.latest: true. If the latest flag is omitted, all the pacts with the specified tag will be returned.
(This might seem a bit weird, but it's done this way to match the syntax used for the matrix query params. See https://docs.pact.io/selectors)

consumerVersionSelectors.consumer: allows a selector to only be applied to a certain consumer.
This is used for example when there is an API that has multiple consumers, one of which is a deployed service, and one of which is a mobile consumer.
The deployed service only needs the latest production pact verified, where as the mobile consumer may want all the production pacts verified.
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

sealed trait Authentication

object Authentication {
  final case class BasicAuth(user: String, pass: String) extends Authentication
  final case class TokenAuth(token: String)              extends Authentication
}
