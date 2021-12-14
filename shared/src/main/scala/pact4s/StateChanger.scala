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

import cask.main.Main
import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import pact4s.provider.ProviderState

import java.io.StringReader
import javax.json.Json
import scala.annotation.nowarn
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
      extends cask.Routes
      with StateChanger {
    private var stopServer: () => Unit      = () => ()
    private var interruptThread: () => Unit = () => ()

    locally(endpoint)

    def allRoutes = Seq(this)

    private implicit def log: cask.util.Logger = new cask.util.Logger.Console()

    private def defaultHandler = new BlockingHandler(
      new Main.DefaultHandler(
        Main.prepareDispatchTrie(allRoutes),
        Nil,
        true,
        Main.defaultHandleNotFound,
        Main.defaultHandleMethodNotAllowed,
        Main.defaultHandleError(_, _, _, debugMode = true)
      )
    )

    private def getState(input: String): Option[ProviderState] =
      Try {
        val parser = Json.createParser(new StringReader(input))
        parser.next()
        val obj         = parser.getObject
        val maybeParams = Option(obj.getJsonObject("params"))
        val _           = maybeParams.map(_.entrySet().asScala.map(kv => kv.getKey -> kv.getValue.toString).toMap)
        obj.getString("state")
      }.toOption.map(ProviderState)

    @nowarn
    @cask.post(endpoint)
    def stateChange(request: cask.Request): Unit =
      getState(request.text()).flatMap(stateChange.lift).getOrElse(())

    def start(): Unit = {
      initialize()
      val r = new Runnable {
        override def run(): Unit = {
          val server = Undertow.builder
            .addHttpListener(port, host)
            .setHandler(defaultHandler)
            .build
          stopServer = () => server.stop()
          server.start()
        }
      }
      val t = new Thread(r)
      interruptThread = () => t.interrupt()
      t.start()
    }

    def shutdown(): Unit = {
      stopServer()
      interruptThread()
    }
  }
}
