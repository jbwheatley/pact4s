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

import scala.annotation.nowarn

private[pact4s] sealed trait StateChanger {
  def start(): Unit
  def shutdown(): Unit
}

private[pact4s] object StateChanger {
  object NoOpStateChanger extends StateChanger {
    def start(): Unit    = ()
    def shutdown(): Unit = ()
  }

  class SimpleServer(stateChange: PartialFunction[ProviderState, Unit]) extends cask.Routes with StateChanger {
    private var server: Undertow = _

    def allRoutes = Seq(this)

    private val port: Int = 64646

    private val host: String = "localhost"

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

    private def getState(input: String): Option[ProviderState] = {
      var state: Option[String] = None
      var started               = false
      var complete              = false
      var outOfInput            = false
      var idx                   = input.indexOf("\"state\"") + 7
      if (idx < 7) ()
      else {
        while (!complete && !outOfInput)
          try {
            val c = input(idx)
            if (!started && c == '\"') {
              started = true
              state = Some("")
              idx += 1
            } else if (started && c == '\"') {
              complete = true
            } else {
              state = state.map(_ + c)
              idx += 1
            }
          } catch {
            case _: Throwable => outOfInput = true
          }
      }
      state.map(ProviderState)
    }

    @nowarn
    @cask.post("/pact4s-state-change")
    def stateChange(request: cask.Request): Unit =
      getState(request.text()).flatMap(stateChange.lift).getOrElse(())

    def start(): Unit = {
      initialize()
      val r = new Runnable {
        override def run(): Unit = {
          val _server = Undertow.builder
            .addHttpListener(port, host)
            .setHandler(defaultHandler)
            .build
          server = _server
          _server.start()
        }
      }
      new Thread(r).start()
    }

    def shutdown(): Unit =
      server.stop()
  }
}
