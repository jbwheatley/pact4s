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

package pact4s.scalatest.message

import au.com.dius.pact.provider.reporters.VerifierReporter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pact4s.MockProviderServer
import pact4s.messages.MessagesProvider
import pact4s.provider.ProviderInfoBuilder
import pact4s.scalatest.MessagePactVerifier

import java.lang.reflect.{InvocationHandler, Method, Proxy}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._

/** Exercises the `additionalReporters` parameter of `verifyPacts`. The probe is a `java.lang.reflect.Proxy` over
  * `VerifierReporter` that no-ops every callback except `reportVerificationForConsumer`, which records its invocations.
  * If `additionalReporters` is wired into `ProviderVerifier.setReporters` correctly, the counter must increment at
  * least once during a verification that has at least one consumer. The Proxy approach avoids a ~30-method no-op
  * subclass that would otherwise be needed because `BaseVerifierReporter` leaves the bulk of `VerifierReporter`
  * abstract and all concrete reporters in pact-jvm are `final`.
  */
class AdditionalReportersFileSuite extends AnyFlatSpec with Matchers with MessagePactVerifier {
  lazy val mock = new MockProviderServer(49158)

  def messages: ResponseFactory     = MessagesProvider.messages
  def provider: ProviderInfoBuilder = mock.fileSourceMessageProviderInfo

  it should "register additional reporters and route per-consumer callbacks through them" in {
    val consumers = new ConcurrentLinkedQueue[String]()

    val handler = new InvocationHandler {
      override def invoke(proxy: Any, method: Method, args: Array[AnyRef]): AnyRef =
        method.getName match {
          case "reportVerificationForConsumer" =>
            // args(0) is IConsumerInfo — record its name
            val consumer = args(0)
            val getName  = consumer.getClass.getMethod("getName")
            consumers.add(getName.invoke(consumer).asInstanceOf[String])
            null
          case "equals"   => java.lang.Boolean.valueOf(proxy eq args(0))
          case "hashCode" => java.lang.Integer.valueOf(System.identityHashCode(proxy))
          case "toString" => "additionalReporters-probe"
          case _          =>
            // Return a sensible default for the method's return type.
            method.getReturnType match {
              case t if t == classOf[Boolean] || t == java.lang.Boolean.TYPE => java.lang.Boolean.FALSE
              case t if t == classOf[Int] || t == java.lang.Integer.TYPE     => java.lang.Integer.valueOf(0)
              case t if t == classOf[Long] || t == java.lang.Long.TYPE       => java.lang.Long.valueOf(0L)
              case t if t == java.lang.Void.TYPE                             => null
              case _                                                         => null
            }
        }
    }

    val probe = Proxy
      .newProxyInstance(getClass.getClassLoader, Array(classOf[VerifierReporter]), handler)
      .asInstanceOf[VerifierReporter]

    verifyPacts(additionalReporters = List(probe))

    // Asserts wiring (probe was invoked), exactly-once-per-consumer routing,
    // and that the right IConsumerInfo flowed through (name pinned to fixture).
    consumers.asScala.toList shouldBe List("Pact4sMessageConsumer")
  }
}
