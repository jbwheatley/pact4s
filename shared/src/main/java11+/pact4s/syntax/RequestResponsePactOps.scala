/*
 * Copyright 2021-2021 io.github.jbwheatley
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
package syntax

import au.com.dius.pact.consumer.dsl._
import org.apache.hc.core5.http.ContentType
import RequestResponsePactOpsForPlatform._
import pact4s.algebras.PactBodyJsonEncoder
import pact4s.syntax.RequestResponsePactOps._

import scala.jdk.CollectionConverters._

object RequestResponsePactOps {
  class PactDslRequestWithPathOps(val builder: PactDslRequestWithPath) extends AnyVal {
    def headers(headers: Map[String, String]): PactDslRequestWithPath = builder.headers(headers.asJava)

    def headers(header: (String, String), rest: (String, String)*): PactDslRequestWithPath =
      this.headers(rest.toMap + header)

    def body[A](body: A)(implicit ev: PactBodyJsonEncoder[A]): PactDslRequestWithPath =
      builder.body(ev.toJsonString(body))
    def body[A](body: A, mimeType: String)(implicit ev: PactBodyJsonEncoder[A]): PactDslRequestWithPath =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType))
    def body[A](body: A, mimeType: String, charset: String)(implicit
        ev: PactBodyJsonEncoder[A]
    ): PactDslRequestWithPath =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType, charset))
  }

  class PactDslRequestWithoutPathOps(val builder: PactDslRequestWithoutPath) extends AnyVal {
    def headers(headers: Map[String, String]): PactDslRequestWithoutPath = builder.headers(headers.asJava)

    def headers(header: (String, String), rest: (String, String)*): PactDslRequestWithoutPath =
      this.headers(rest.toMap + header)

    def body[A](body: A)(implicit ev: PactBodyJsonEncoder[A]): PactDslRequestWithoutPath =
      builder.body(ev.toJsonString(body))
    def body[A](body: A, mimeType: String)(implicit ev: PactBodyJsonEncoder[A]): PactDslRequestWithoutPath =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType))
    def body[A](body: A, mimeType: String, charset: String)(implicit
        ev: PactBodyJsonEncoder[A]
    ): PactDslRequestWithoutPath =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType, charset))
  }

  class PactDslResponseOps(val builder: PactDslResponse) extends AnyVal {
    def headers(headers: Map[String, String]): PactDslResponse = builder.headers(headers.asJava)

    def headers(header: (String, String), rest: (String, String)*): PactDslResponse =
      this.headers(rest.toMap + header)

    def body[A](body: A)(implicit ev: PactBodyJsonEncoder[A]): PactDslResponse = builder.body(ev.toJsonString(body))
    def body[A](body: A, mimeType: String)(implicit ev: PactBodyJsonEncoder[A]): PactDslResponse =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType))
    def body[A](body: A, mimeType: String, charset: String)(implicit ev: PactBodyJsonEncoder[A]): PactDslResponse =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType, charset))
  }
}

trait RequestResponsePactOps {
  implicit def toPactDslWithProviderOps(builder: PactDslWithProvider): PactDslWithProviderOps =
    new PactDslWithProviderOps(builder)
  implicit def toPactDslRequestWithPathOps(builder: PactDslRequestWithPath): PactDslRequestWithPathOps =
    new PactDslRequestWithPathOps(builder)
  implicit def toPactDslRequestWithoutPathOps(builder: PactDslRequestWithoutPath): PactDslRequestWithoutPathOps =
    new PactDslRequestWithoutPathOps(builder)
  implicit def toPactDslResponseOps(builder: PactDslResponse): PactDslResponseOps =
    new PactDslResponseOps(builder)
  implicit def toPactDslResponseOpsForPlatform(builder: PactDslResponse): PactDslResponseOpsForPlatform =
    new PactDslResponseOpsForPlatform(builder)
  implicit def toPactDslWithStateOps(builder: PactDslWithState): PactDslWithStateOps =
    new PactDslWithStateOps(builder)
}
