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
import au.com.dius.pact.core.pactbroker.{ConsumerVersionSelector => PactJVMSelector}
import au.com.dius.pact.provider.{PactVerification, ProviderInfo}
import org.apache.http.HttpRequest
import org.apache.http.message.BasicHeader
import pact4s.Authentication.{BasicAuth, TokenAuth}
import pact4s.PactSource.PactBrokerWithSelectors.ProviderTags
import pact4s.PactSource.{FileSource, PactBroker, PactBrokerWithSelectors, PactBrokerWithTags}
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings

import java.io.File
import java.net.{URI, URL}
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, OffsetDateTime, ZoneOffset}
import java.util.function.Consumer
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

/** Interface for defining the provider that consumer pacts are verified against. Internally gets converted to au.com.dius.pact.provider.ProviderInfo
  * during verification.
  *
  * @param name the name of the provider
  * @param protocol
  * @param host
  * @param port
  * @param path
  * address of the mock provider server is {protocol}://{host}:{port}{path}
  * @param pactSource pacts to verify can come either from a file location, or from a pact broker.
  * @param stateChangeUrl full url of the mock provider endpoint that can be used for setting provider state before each pact with state is run.
  * state is sent as JSON of the form {"state": "state goes here"}. Can also be set using [[ProviderInfoBuilder#withStateChangeEndpoint]] just by providing the path.
  * @param verificationSettings Required if verifying message pacts
  *
  * @param requestFilter Apply filters to certain consumer requests. The most common use case for this is adding auth headers to requests
  * @see https://docs.pact.io/faq/#how-do-i-test-oauth-or-other-security-headers
  */
final case class ProviderInfoBuilder(
    name: String,
    protocol: String,
    host: String,
    port: Int,
    path: String,
    pactSource: PactSource,
    stateChangeUrl: Option[String] = None,
    verificationSettings: Option[VerificationSettings] = None,
    requestFilter: ProviderRequest => List[ProviderRequestFilter] = _ => Nil
) {
  def withProtocol(protocol: String): ProviderInfoBuilder = this.copy(protocol = protocol)
  def withHost(host: String): ProviderInfoBuilder         = this.copy(host = host)
  def withPort(port: Int): ProviderInfoBuilder            = this.copy(port = port)
  def withPath(path: String): ProviderInfoBuilder         = this.copy(path = path)
  def withVerificationSettings(settings: VerificationSettings): ProviderInfoBuilder =
    this.copy(verificationSettings = Some(settings))
  def withOptionalVerificationSettings(settings: Option[VerificationSettings]): ProviderInfoBuilder =
    this.copy(verificationSettings = settings)
  def withStateChangeUrl(url: String): ProviderInfoBuilder = this.copy(stateChangeUrl = Some(url))
  def withStateChangeEndpoint(endpoint: String): ProviderInfoBuilder = {
    val endpointWithLeadingSlash = if (!endpoint.startsWith("/")) "/" + endpoint else endpoint
    this.copy(stateChangeUrl = Some(s"$protocol://$host:$port$endpointWithLeadingSlash"))
  }
  def withRequestFilter(requestFilter: ProviderRequest => List[ProviderRequestFilter]): ProviderInfoBuilder =
    this.copy(requestFilter = requestFilter)

  private def pactJvmRequestFilter: HttpRequest => Unit = { request =>
    val requestLine = request.getRequestLine
    val providerRequest = ProviderRequest(
      requestLine.getMethod,
      new URI(requestLine.getUri),
      request.getAllHeaders.toList.map(h => (h.getName, h.getValue))
    )
    requestFilter(providerRequest).foreach(_.filter(request))
  }

  private[pact4s] def toProviderInfo: ProviderInfo = {
    val p = new ProviderInfo(name, protocol, host, port, path)
    verificationSettings.foreach { case AnnotatedMethodVerificationSettings(packagesToScan) =>
      p.setVerificationType(PactVerification.ANNOTATED_METHOD)
      p.setPackagesToScan(packagesToScan.asJava)
    }
    stateChangeUrl.foreach(s => p.setStateChangeUrl(new URL(s)))
    p.setRequestFilter {
      //because java
      new Consumer[HttpRequest] {
        def accept(t: HttpRequest): Unit =
          pactJvmRequestFilter(t)
      }
    }
    pactSource match {
      case broker: PactBroker => applyBrokerSourceToProvider(p, broker)
      case FileSource(consumers) =>
        consumers.foreach { case (consumer, file) =>
          p.hasPactWith(
            consumer,
            { consumer =>
              consumer.setPactSource(new PactJVMFileSource(file))
              kotlin.Unit.INSTANCE
            }
          )
        }
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
        options.put("providerTags", providerTags.toList.asJava)
        auth.foreach {
          case TokenAuth(token)      => options.put("authentication", List("bearer", token).asJava)
          case BasicAuth(user, pass) => options.put("authentication", List("basic", user, pass).asJava)
        }
        includeWipPactsSince.foreach(since => options.put("includeWipPactsSince", instantToDateString(since)))
        providerInfo.hasPactsFromPactBrokerWithSelectors(options, brokerUrl, selectors.map(_.toPactJVMSelector).asJava)
        providerInfo
      case PactBrokerWithTags(brokerUrl, auth, tags) =>
        applyBrokerSourceToProvider(
          providerInfo,
          PactBrokerWithSelectors(
            brokerUrl
          ).withOptionalAuth(auth).withSelectors(tags.map(tag => ConsumerVersionSelector().withTag(tag)))
        )
    }

  private def instantToDateString(instant: Instant): String =
    instant
      .atOffset(
        ZoneOffset.UTC //Apologies for the euro-centrism, but as we use time relative to the epoch it doesn't really matter
      )
      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

object ProviderInfoBuilder {

  /** Auxiliary constructor that provides some common defaults for the the mock provider address
    *
    * @param name [[ProviderInfoBuilder.name]]
    * @param pactSource [[ProviderInfoBuilder.pactSource]]
    * @return [[ProviderInfoBuilder]]
    *
    * Example usage:
    * {{{
    *  ProviderInfoBuilder(
    *      name = "Provider Service",
    *      pactSource = FileSource("Consumer Service", new File("./pacts/pact.json"))
    *    ).withPort(80)
    *     .withStateChangeEndpoint("setup")
    * }}}
    */
  def apply(name: String, pactSource: PactSource): ProviderInfoBuilder =
    ProviderInfoBuilder(name, "http", "localhost", 0, "/", pactSource)
}

sealed trait VerificationSettings

object VerificationSettings {

  /** For verifying message pacts, pact-jvm searches across the classpath for uniquely defined annotated methods that represent
    * the message produced by the provider. For example:
    * {{{
    *  @PactVerifyProvider("A message to say goodbye")
    *  def goodbyeMessage(): MessageAndMetadata = {
    *    val body = """{"goodbye":"harry"}"""
    *    MessageAndMetadataBuilder(body).build
    *  }
    * }}}
    *
    * @param packagesToScan which packages to scan, e.g. "pact4s.messages"
    */
  final case class AnnotatedMethodVerificationSettings(packagesToScan: List[String]) extends VerificationSettings
}

sealed trait PactSource

object PactSource {

  /** It isn't necessary to use a pact broker to manage consumer pacts (though it is strongly recommended). The pacts can also
    * be directly loaded from files by using this pactSource as [[ProviderInfoBuilder.pactSource]]
    */
  final case class FileSource(consumers: Map[String, File]) extends PactSource

  sealed trait PactBroker extends PactSource {
    def brokerUrl: String
    def auth: Option[Authentication]
  }

  /** @param brokerUrl pact broker address
    * @param auth authentication for accessing the pact broker. Can be token or basic auth.
    * @param tags fetches all the latest pacts from the pact-broker for the provider with the given tags, all ignoring tags if [[tags]] is empty.
    */
  final case class PactBrokerWithTags(brokerUrl: String, auth: Option[Authentication] = None, tags: List[String] = Nil)
      extends PactBroker {
    def withAuth(auth: Authentication): PactBrokerWithTags = this.copy(auth = Some(auth))
    def withoutAuth: PactBrokerWithTags                    = this.copy(auth = None)
    def withTags(tags: List[String]): PactBrokerWithTags   = this.copy(tags = tags)
    def withTags(tags: String*): PactBrokerWithTags        = this.copy(tags = tags.toList)
  }

  /** @param brokerUrl pact broker address
    * @param auth authentication for accessing the pact broker. Can be token or basic auth.
    *
    * @see https://docs.pact.io/pact_broker/advanced_topics/pending_pacts for information on pending and WIP pacts
    *
    * @param enablePending enable pending pacts. Off by default. If enabled, [[providerTags]] must be provided.
    * @see also the master issue for pending pacts https://github.com/pact-foundation/pact_broker/issues/320
    *
    * @param includeWipPactsSince is a [[java.time.Instant]], but can also be set using a [[java.time.LocalDate]] or [[java.time.OffsetDateTime]] for convenience.
    * All WIP pacts are pending pacts, so we enforce the setting of [[enablePending]] if this field is set.
    * @see also the master issue for WIP pacts here for more discussion https://github.com/pact-foundation/pact_broker/issues/338
    *
    * @param providerTags any tags that are going to be applied to the provider version when the verification results are published. Only used in the pending pact
    * calculation, so will get set to empty when passed to pact-jvm if [[enablePending]] is set to false.
    *
    * @param selectors specifies which consumer pacts should be chosen for verification
    *
    * Example:
    * {{{
    *   PactBrokerWithSelectors(
    *     brokerUrl = "https://test.pact.dius.com.au"
    *   ).withAuth(BasicAuth("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1"))
    *    .withPendingPacts(enabled = true)
    *    .withSelectors(ConsumerVersionSelector())
    * }}}
    */
  sealed abstract case class PactBrokerWithSelectors(
      brokerUrl: String,
      auth: Option[Authentication] = None,
      enablePending: Boolean = false,
      includeWipPactsSince: Option[Instant] = None,
      providerTags: Option[ProviderTags] = None,
      selectors: List[ConsumerVersionSelector] = List(ConsumerVersionSelector())
  ) extends PactBroker {
    private def copy(
        brokerUrl: String = brokerUrl,
        auth: Option[Authentication] = auth,
        enablePending: Boolean = enablePending,
        includeWipPactsSince: Option[Instant] = includeWipPactsSince,
        providerTags: Option[ProviderTags] = providerTags,
        selectors: List[ConsumerVersionSelector] = selectors
    ): PactBrokerWithSelectors =
      new PactBrokerWithSelectors(brokerUrl, auth, enablePending, includeWipPactsSince, providerTags, selectors) {}

    private[pact4s] def withOptionalAuth(auth: Option[Authentication]): PactBrokerWithSelectors = copy(auth = auth)

    def withAuth(auth: Authentication): PactBrokerWithSelectors = copy(auth = Some(auth))

    def withPendingPactsEnabled(providerTags: ProviderTags): PactBrokerWithSelectors =
      copy(enablePending = true, providerTags = Some(providerTags))

    def withPendingPactsDisabled: PactBrokerWithSelectors =
      copy(enablePending = false, includeWipPactsSince = None, providerTags = None)

    def withWipPactsSince(since: Instant, providerTags: ProviderTags): PactBrokerWithSelectors =
      copy(enablePending = true, includeWipPactsSince = Some(since), providerTags = Some(providerTags))

    def withWipPactsSince(since: LocalDate, providerTags: ProviderTags): PactBrokerWithSelectors =
      withWipPactsSince(since.atStartOfDay().toInstant(ZoneOffset.UTC), providerTags)

    def withWipPactsSince(since: OffsetDateTime, providerTags: ProviderTags): PactBrokerWithSelectors =
      withWipPactsSince(since.toInstant, providerTags)

    def withSelectors(selectors: List[ConsumerVersionSelector]): PactBrokerWithSelectors =
      copy(selectors = selectors)

    def withSelectors(selectors: ConsumerVersionSelector*): PactBrokerWithSelectors =
      copy(selectors = selectors.toList)
  }

  object PactBrokerWithSelectors {
    def apply(
        brokerUrl: String
    ): PactBrokerWithSelectors =
      new PactBrokerWithSelectors(brokerUrl = brokerUrl) {}

    @deprecated(message = "Use the other apply method with the safer builder patterns", since = "0.0.17")
    def apply(
        brokerUrl: String,
        auth: Option[Authentication] = None,
        enablePending: Boolean = false,
        includeWipPactsSince: Option[FiniteDuration] = None,
        providerTags: List[String] = Nil,
        selectors: List[ConsumerVersionSelector] = List(ConsumerVersionSelector())
    ): PactBrokerWithSelectors = {
      val tags = providerTags match {
        case head :: tail => Some(ProviderTags(head, tail))
        case Nil          => None
      }
      new PactBrokerWithSelectors(
        brokerUrl,
        auth,
        enablePending,
        includeWipPactsSince.map(d => Instant.ofEpochSecond(d.toSeconds)),
        tags,
        selectors
      ) {}
    }

    final case class ProviderTags(head: String, tail: List[String]) {
      def ::(tag: String): ProviderTags        = ProviderTags(tag, head :: tail)
      private[pact4s] def toList: List[String] = head :: tail
    }

    object ProviderTags {
      def one(tag: String): ProviderTags = ProviderTags(tag, Nil)
    }
  }
}

/** @see https://docs.pact.io/pact_broker/advanced_topics/consumer_version_selectors/ and https://github.com/pact-foundation/pact_broker/issues/307
  * Gets converted into a au.com.dius.pact.core.pactbroker.ConsumerVersionSelector under the hood.
  *
  * @param tag the tag name(s) of the consumer versions to get the pacts for.
  * @param latest the name of the tag to fallback to if the specified tag does not exist.
  * This is useful when the consumer and provider use matching branch names to coordinate the development of new features.
  * @param fallbackTag default true. If the latest flag is omitted, all the pacts with the specified tag will be returned.
  * (This might seem a bit weird, but it's done this way to match the syntax used for the matrix query params. See https://docs.pact.io/selectors)
  * @param consumer allows a selector to only be applied to a certain consumer.
  * This is used for example when there is an API that has multiple consumers, one of which is a deployed service, and one of which is a mobile consumer.
  * The deployed service only needs the latest production pact verified, where as the mobile consumer may want all the production pacts verified.
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

/** Auth for accessing a pact-broker */
sealed trait Authentication

object Authentication {
  final case class BasicAuth(user: String, pass: String) extends Authentication
  final case class TokenAuth(token: String)              extends Authentication
}

/** A simplified interface for managing the requests sent by pact-jvm to the mock provider service.
  * Used in conjunction with [[ProviderRequestFilter]] in [[ProviderInfoBuilder.requestFilter]]
  */
final case class ProviderRequest private[pact4s] (method: String, uri: URI, headers: List[(String, String)]) {
  def containsHeaders(name: String): Boolean           = headers.exists(_._1 == name)
  def getHeaders(name: String): List[(String, String)] = headers.filter(_._1 == name)
  def getFirstHeader(name: String): Option[(String, String)] = headers.collectFirst {
    case (n, value) if n == name => (n, value)
  }
  def getLastHeader(name: String): Option[(String, String)] = headers.reverse.collectFirst {
    case (n, value) if n == name => (n, value)
  }
}

/** pact-jvm uses apache-http as its http implementation. A ProviderRequestFilter applies a transformation to a [[org.apache.http.HttpRequest]]
  * before passing it to the mock provider server. Its called a "filter" as this is what its called in other pact implementations. It doesn't
  * filter in the sense of stopping certain requests from reaching the mock server.
  */
trait ProviderRequestFilter {
  def filter(request: HttpRequest): Unit
}

object ProviderRequestFilter {
  sealed abstract class AddHeaders(headers: List[(String, String)]) extends ProviderRequestFilter {
    def filter(request: HttpRequest): Unit = headers.foreach { case (name, value) => request.addHeader(name, value) }
  }

  object AddHeaders {
    def apply(first: (String, String), rest: (String, String)*): AddHeaders = new AddHeaders(first :: rest.toList) {}
  }

  sealed abstract class SetHeaders(headers: List[(String, String)]) extends ProviderRequestFilter {
    def filter(request: HttpRequest): Unit = headers.foreach { case (name, value) =>
      request.setHeader(name, value)
    }
  }

  object SetHeaders {
    def apply(first: (String, String), rest: (String, String)*): SetHeaders = new SetHeaders(first :: rest.toList) {}
  }

  sealed abstract class OverwriteHeaders(headers: List[(String, String)]) extends ProviderRequestFilter {
    def filter(request: HttpRequest): Unit = request.setHeaders(headers.map { case (name, value) =>
      new BasicHeader(name, value)
    }.toArray)
  }

  object OverwriteHeaders {
    def apply(headers: (String, String)*): OverwriteHeaders = new OverwriteHeaders(headers.toList) {}
  }

  sealed abstract class RemoveHeaders(headerNames: List[String]) extends ProviderRequestFilter {
    override def filter(request: HttpRequest): Unit = headerNames.foreach(request.removeHeaders)
  }

  object RemoveHeaders {
    def apply(first: String, rest: String*): RemoveHeaders = new RemoveHeaders(first :: rest.toList) {}
  }
}
