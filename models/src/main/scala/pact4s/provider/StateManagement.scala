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

sealed trait StateManagement {
  private[provider] def url: String
}

object StateManagement {
  final case class ProviderUrl(private[provider] val url: String) extends StateManagement

  final class StateManagementFunction private (
      private[pact4s] val stateChangeFunc: PartialFunction[ProviderState, Unit],
      private[pact4s] val stateChangeBeforeHook: () => Unit,
      private[pact4s] val host: String,
      private[pact4s] val port: Int,
      private[pact4s] val endpoint: String
  ) extends StateManagement {
    def withOverrides(
        hostOverride: String = host,
        portOverride: Int = port,
        endpointOverride: String = endpoint
    ): StateManagementFunction =
      new StateManagementFunction(
        stateChangeFunc,
        stateChangeBeforeHook,
        host = hostOverride,
        port = portOverride,
        endpoint = endpointOverride
      )

    def withBeforeEach(beforeHook: () => Unit): StateManagementFunction =
      new StateManagementFunction(stateChangeFunc, beforeHook, host, port, endpoint)

    private val slashedEndpoint       = if (!endpoint.startsWith("/")) "/" + endpoint else endpoint
    private[provider] val url: String = s"http://$host:$port$slashedEndpoint"
  }

  object StateManagementFunction {

    def apply(
        stateChangeFunc: PartialFunction[ProviderState, Unit]
    ): StateManagementFunction =
      new StateManagementFunction(stateChangeFunc, () => (), "localhost", 64646, "/pact4s-state-change")

  }
}
