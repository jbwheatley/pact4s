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

import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.message.BasicHeader

/** pact-jvm uses apache-http as its http implementation. A ProviderRequestFilter applies a transformation to a
  * [[org.apache.hc.core5.http.HttpRequest]] before passing it to the mock provider server. Its called a "filter" here
  * as this is what its called in other pact implementations. It doesn't filter in the sense of stopping certain
  * requests from reaching the mock server.
  */
trait ProviderRequestFilter {
  protected def filter(request: HttpRequest): Unit
  def andThen(that: ProviderRequestFilter): ProviderRequestFilter = (request: HttpRequest) => {
    this.filter(request)
    that.filter(request)
  }
}

object ProviderRequestFilter {
  val NoOpFilter: ProviderRequestFilter = (_: HttpRequest) => ()

  sealed abstract class AddHeaders(headers: List[(String, String)]) extends ProviderRequestFilter {
    protected def filter(request: HttpRequest): Unit = headers.foreach { case (name, value) =>
      request.addHeader(name, value)
    }
  }

  object AddHeaders {
    def apply(first: (String, String), rest: (String, String)*): AddHeaders = new AddHeaders(first :: rest.toList) {}
  }

  sealed abstract class SetHeaders(headers: List[(String, String)]) extends ProviderRequestFilter {
    protected def filter(request: HttpRequest): Unit = headers.foreach { case (name, value) =>
      request.setHeader(name, value)
    }
  }

  object SetHeaders {
    def apply(first: (String, String), rest: (String, String)*): SetHeaders = new SetHeaders(first :: rest.toList) {}
  }

  sealed abstract class OverwriteHeaders(headers: List[(String, String)]) extends ProviderRequestFilter {
    protected def filter(request: HttpRequest): Unit = request.setHeaders(headers.map { case (name, value) =>
      new BasicHeader(name, value)
    }: _*)
  }

  object OverwriteHeaders {
    def apply(headers: (String, String)*): OverwriteHeaders = new OverwriteHeaders(headers.toList) {}
  }

  sealed abstract class RemoveHeaders(headerNames: List[String]) extends ProviderRequestFilter {
    protected def filter(request: HttpRequest): Unit = headerNames.foreach(request.removeHeaders)
  }

  object RemoveHeaders {
    def apply(first: String, rest: String*): RemoveHeaders = new RemoveHeaders(first :: rest.toList) {}
  }
}
