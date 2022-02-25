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

import java.net.InetSocketAddress
import javax.json.{Json, JsonValue}
import scala.jdk.CollectionConverters._
import scala.util.Try

private[pact4s] sealed trait StateChanger {
  def start(): Unit
  def shutdown(): Unit
}

private[pact4s] object StateChanger {
  object NoOpStateChanger extends StateChanger {
    def start(): Unit    = ()
    def shutdown(): Unit = ()
  }

  class SimpleServer(stateChange: PartialFunction[ProviderState, Unit], host: String, port: Int, endpoint: String)
      extends StateChanger {
    private var isShutdown: Boolean    = true
    private var stopServer: () => Unit = () => ()

    def start(): Unit = {
      val server          = HttpServer.create(new InetSocketAddress(host, port), 0)
      val slashedEndpoint = if (endpoint.startsWith("/")) endpoint else "/" + endpoint
      server.createContext(slashedEndpoint, RootHandler)
      server.setExecutor(null)
      stopServer = () => server.stop(0)
      isShutdown = false
      server.start()
    }

    def shutdown(): Unit =
      if (!isShutdown) {
        stopServer()
        isShutdown = true
      }

    object RootHandler extends HttpHandler {
      def handle(t: HttpExchange): Unit = {
        Try {
          val parser = Json.createParser(t.getRequestBody)
          parser.next()
          val obj         = parser.getObject
          val maybeParams = Option(obj.getJsonObject("params"))
          // This needs work.
          val params: Map[String, String] = maybeParams
            .map(_.entrySet().asScala.map { kv =>
              val key   = kv.getKey
              val value = kv.getValue
              val fixedValue = value.getValueType match {
                case JsonValue.ValueType.STRING => value.toString.init.tail
                case _                          => value.toString
              }
              key -> fixedValue
            }.toMap)
            .getOrElse(Map.empty)
          (obj.getString("state"), params)
        }.toOption.map { case (s, ps) => ProviderState(s, ps) }.flatMap(stateChange.lift).getOrElse(())
        sendResponse(t)
      }

      private def sendResponse(t: HttpExchange): Unit = {
        val response = "Ack!"
        t.sendResponseHeaders(200, response.length().toLong)
        val os = t.getResponseBody
        os.write(response.getBytes)
        os.close()
      }
    }
  }
}
