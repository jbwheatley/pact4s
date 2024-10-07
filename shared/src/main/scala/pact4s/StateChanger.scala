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

import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import jakarta.json.{Json, JsonObject, JsonValue}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

private[pact4s] sealed trait StateChanger {
  def start(): Unit
  def shutdown(): Unit
}

private[pact4s] object StateChanger {
  object NoOpStateChanger extends StateChanger {
    def start(): Unit    = ()
    def shutdown(): Unit = ()
  }

  class SimpleServer(
      stateChange: PartialFunction[ProviderState, Unit],
      stateChangeBeforeHook: () => Unit,
      host: String,
      port: Int,
      endpoint: String
  ) extends StateChanger {
    private var isShutdown: Boolean = true
    private var _server: HttpServer = _

    def start(): Unit = {
      val server = HttpServer.create(new InetSocketAddress(host, port), 0)
      _server = server
      val slashedEndpoint = if (endpoint.startsWith("/")) endpoint else "/" + endpoint
      server.createContext(slashedEndpoint, RootHandler)
      server.setExecutor(null)
      isShutdown = false
      server.start()
    }

    def shutdown(): Unit =
      if (!isShutdown) {
        _server.stop(0)
        isShutdown = true
      }

    private object RootHandler extends HttpHandler with Pact4sLogger {

      def handle(t: HttpExchange): Unit = {
        val stateAndResponse: Option[(String, Map[String, String], String)] = Try {
          val parser = Json.createParser(t.getRequestBody)
          parser.next()
          val obj                             = parser.getObject
          val maybeParams: Option[JsonObject] = Option(obj.getJsonObject("params"))
          // This needs work.
          val params: Map[String, String] = maybeParams
            .map(
              _.entrySet().asScala
                .map { kv =>
                  val key   = kv.getKey
                  val value = kv.getValue
                  val fixedValue = value.getValueType match {
                    case JsonValue.ValueType.STRING => value.toString.init.tail
                    case _                          => value.toString.replace("\"", "\\\"")
                  }
                  key -> fixedValue
                }
                .toMap
            )
            .getOrElse(Map.empty)

          // should return the params in the response body to be used with the generators
          val body = maybeParams
            .map { ps =>
              val os     = new ByteArrayOutputStream()
              val writer = Json.createWriter(os)
              writer.writeObject(ps)
              writer.close()
              os.toString
            }
            .getOrElse("{}")

          (obj.getString("state"), params, body)
        }.toOption

        val stateChangeMaybeApplied = Try(stateAndResponse.foreach { case (s, ps, _) =>
          // Apply before hook
          stateChangeBeforeHook.apply()
          // Apply state change function
          stateChange
            .lift(ProviderState(s, ps))
            .getOrElse(
              pact4sLogger.warn(s"No state change definition was provided for received state $s with parameters $ps")
            )
        })
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
