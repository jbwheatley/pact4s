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

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import pact4s.provider.ProviderState
import ujson.Value
import upickle.core.LinkedHashMap
import upickle.default._

import java.net.InetSocketAddress
import scala.util.{Failure, Success, Try}

private[pact4s] sealed trait StateChanger {
  def start(): Unit
  def shutdown(): Unit
}

private[pact4s] object StateChanger {
  class SimpleServer(
      stateChange: PartialFunction[ProviderState, Unit],
      stateChangeBeforeHook: () => Unit,
      host: String,
      port: Int,
      endpoint: String
  ) extends StateChanger
      with Pact4sLogger {
    private var isShutdown: Boolean = true
    private var _server: HttpServer = _
    var boundPort: Int              = _

    def start(): Unit = {
      val server = HttpServer.create(new InetSocketAddress(host, port), 0)
      _server = server
      val slashedEndpoint = if (endpoint.startsWith("/")) endpoint else "/" + endpoint
      server.createContext(slashedEndpoint, RootHandler)
      server.setExecutor(null)
      isShutdown = false
      server.start()
      boundPort = server.getAddress.getPort
      println(s"State change server bound to address $host:$boundPort:$slashedEndpoint")
      pact4sLogger.info(s"State change server bound to address $host:$boundPort:$slashedEndpoint")
    }

    def shutdown(): Unit =
      if (!isShutdown) {
        _server.stop(0)
        isShutdown = true
        pact4sLogger.info(s"Shutting down state change server")
      }

    private object RootHandler extends HttpHandler with Pact4sLogger {

      def handle(t: HttpExchange): Unit = {
        val stateAndResponse: Try[(String, Map[String, String], String)] = Try {
          val obj: LinkedHashMap[String, Value]                 = read[ujson.Obj](t.getRequestBody).obj
          val maybeParams: Option[LinkedHashMap[String, Value]] = obj.get("params").flatMap(_.objOpt)
          // This needs work.
          val params: Map[String, String] = maybeParams
            .map(_.toMap)
            .map {
              _.map { case (key, value) =>
                key -> value.strOpt.getOrElse(value.toString())
              }.toMap
            }
            .getOrElse(Map.empty[String, String])

          // should return the params in the response body to be used with the generators
          val body: String = maybeParams
            .map { ps =>
              write(ujson.Obj.from(ps))
            }
            .getOrElse("{}")

          (obj.get("state").map(_.str).getOrElse(""), params, body)
        }

        val stateChangeMaybeApplied = stateAndResponse.flatMap { case (s, ps, _) =>
          Try {
            // Apply before hook
            stateChangeBeforeHook.apply()
            // Apply state change function
            stateChange
              .lift(ProviderState(s, ps))
              .getOrElse(
                pact4sLogger.warn(s"No state change definition was provided for received state $s with parameters $ps")
              )
          }
        }
        stateChangeMaybeApplied match {
          case Failure(exception) =>
            pact4sLogger.error(exception)("State change application failed.")
            sendResponse(t, "{}", 400)
          case Success(_) =>
            val responseBody: String = stateAndResponse
              .map(_._3)
              .getOrElse("{}")
            sendResponse(t, responseBody, 200)
        }
      }

      private def sendResponse(t: HttpExchange, body: String, code: Int): Unit = {
        val response = body
        t.getResponseHeaders.set("Content-Type", "application/json")
        t.sendResponseHeaders(code, response.length().toLong)
        val os = t.getResponseBody
        os.write(response.getBytes)
        os.close()
      }
    }
  }
}
