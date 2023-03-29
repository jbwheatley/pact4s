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

package pact4s.syntax

import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import pact4s.syntax.PactOps.PactBuilderOps

import scala.jdk.CollectionConverters._

object PactOps {
  class PactBuilderOps(val builder: PactBuilder) extends AnyVal {
    def consumer(consumer: String): PactBuilder = {
      builder.setConsumer(consumer)
      builder
    }

    def provider(provider: String): PactBuilder = {
      builder.setProvider(provider)
      builder
    }

    def pactVersion(pactVersion: PactSpecVersion): PactBuilder = {
      builder.setPactVersion(pactVersion)
      builder
    }

    /** Describe the state the provider needs to be in for the pact test to be verified. Any parameters for the provider
      * state can be provided in the second parameter.
      */
    def `given`(state: String, params: Map[String, Any]): PactBuilder = builder.`given`(state, params.asJava)

    /** Values to configure the interaction. In the case of an interaction configured by a plugin, you need to follow
      * the plugin documentation of what values must be specified here.
      */
    def `with`(values: Map[String, Any]): PactBuilder = {
      def valuesToJava(value: Any): Any =
        value match {
          case map: Map[_, _]  => map.map { case (k, v) => (k, valuesToJava(v)) }.asJava
          case it: Iterable[_] => it.map(valuesToJava).asJava
          case other           => other
        }

      builder.`with`(valuesToJava(values).asInstanceOf[java.util.Map[String, Any]])
    }
  }
}

trait PactOps {
  implicit def toPactBuilderOps(builder: PactBuilder): PactBuilderOps = new PactBuilderOps(builder)
}
