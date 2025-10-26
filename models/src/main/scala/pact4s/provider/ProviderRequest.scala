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

import java.net.URI

/** A simplified interface for managing the requests sent by pact-jvm to the mock provider service. Used in conjunction
  * with [[ProviderRequestFilter]] in [[ProviderInfoBuilder.withRequestFiltering]]
  */
final class ProviderRequest private (val method: String, val uri: URI, val headers: List[(String, String)]) {
  def containsHeaders(name: String): Boolean                 = headers.exists(_._1 == name)
  def getHeaders(name: String): List[(String, String)]       = headers.filter(_._1 == name)
  def getFirstHeader(name: String): Option[(String, String)] = headers.collectFirst {
    case (n, value) if n == name => (n, value)
  }
  def getLastHeader(name: String): Option[(String, String)] = headers.reverse.collectFirst {
    case (n, value) if n == name => (n, value)
  }
}

object ProviderRequest {
  private[provider] def apply(method: String, uri: URI, headers: List[(String, String)]): ProviderRequest =
    new ProviderRequest(method, uri, headers)
}
