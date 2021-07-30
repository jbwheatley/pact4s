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
import pact4s.PactSource.{FileSource, PactBroker, PactBrokerWithSelectors, PactBrokerWithTags}
import pact4s.VerificationSettings.AnnotatedMethodVerificationSettings

import java.io.File
import java.net.{URI, URL}
import java.util.Date
import java.util.function.Consumer
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

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

object ProviderInfoBuilder {
  def apply(name: String, pactSource: PactSource): ProviderInfoBuilder =
    ProviderInfoBuilder(name, "http", "localhost", 0, "/", pactSource)
}

sealed trait VerificationSettings

object VerificationSettings {
  final case class AnnotatedMethodVerificationSettings(packagesToScan: List[String]) extends VerificationSettings
}

sealed trait PactSource

object PactSource {
  final case class FileSource(consumer: String, file: File) extends PactSource

  sealed trait PactBroker extends PactSource {
    def brokerUrl: String
    def auth: Option[Authentication]
  }
  final case class PactBrokerWithTags(brokerUrl: String, auth: Option[Authentication] = None, tags: List[String] = Nil)
      extends PactBroker {
    def withAuth(auth: Authentication): PactBrokerWithTags = this.copy(auth = Some(auth))
    def withoutAuth: PactBrokerWithTags                    = this.copy(auth = None)
    def withTags(tags: List[String]): PactBrokerWithTags   = this.copy(tags = tags)
    def withTags(tags: String*): PactBrokerWithTags        = this.copy(tags = tags.toList)
  }
  final case class PactBrokerWithSelectors(
      brokerUrl: String,
      auth: Option[Authentication] = None,
      enablePending: Boolean = false,
      includeWipPactsSince: Option[FiniteDuration] = None,
      providerTags: List[String] = Nil,
      selectors: List[ConsumerVersionSelector] = List(ConsumerVersionSelector())
  ) extends PactBroker {
    def withAuth(auth: Authentication): PactBrokerWithSelectors     = this.copy(auth = Some(auth))
    def withoutAuth: PactBrokerWithSelectors                        = this.copy(auth = None)
    def withPendingPacts(enabled: Boolean): PactBrokerWithSelectors = this.copy(enablePending = enabled)
    def withWipPactsSince(since: FiniteDuration): PactBrokerWithSelectors =
      this.copy(includeWipPactsSince = Some(since))
    def withoutWipPacts: PactBrokerWithSelectors                      = this.copy(includeWipPactsSince = None)
    def withProviderTags(tags: List[String]): PactBrokerWithSelectors = this.copy(providerTags = tags)
    def withProviderTags(tags: String*): PactBrokerWithSelectors      = this.copy(providerTags = tags.toList)
    def withSelectors(selectors: List[ConsumerVersionSelector]): PactBrokerWithSelectors =
      this.copy(selectors = selectors)
    def withSelectors(selectors: ConsumerVersionSelector*): PactBrokerWithSelectors =
      this.copy(selectors = selectors.toList)
  }
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

sealed trait ProviderRequestFilter {
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
