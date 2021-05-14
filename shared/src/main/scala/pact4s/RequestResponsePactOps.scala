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

import au.com.dius.pact.consumer.dsl.{PactDslRequestWithPath, PactDslRequestWithoutPath, PactDslResponse}

import scala.jdk.CollectionConverters.MapHasAsJava
import RequestResponsePactOps._
import org.apache.http.entity.ContentType

object RequestResponsePactOps {
  class PactDslRequestWithPathOps(val builder: PactDslRequestWithPath) extends AnyVal {
    def headers(headers: Map[String, String]): PactDslRequestWithPath = builder.headers(headers.asJava)

    def headers(header: (String, String), rest: (String, String)*): PactDslRequestWithPath =
      this.headers(rest.toMap + header)

    def body[A](body: A)(implicit ev: PactBodyEncoder[A]): PactDslRequestWithPath = builder.body(ev.toJsonString(body))
    def body[A](body: A, mimeType: String)(implicit ev: PactBodyEncoder[A]): PactDslRequestWithPath =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType))
    def body[A](body: A, mimeType: String, charset: String)(implicit
        ev: PactBodyEncoder[A]
    ): PactDslRequestWithPath =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType, charset))
  }

  class PactDslRequestWithoutPathOps(val builder: PactDslRequestWithoutPath) extends AnyVal {
    def headers(headers: Map[String, String]): PactDslRequestWithoutPath = builder.headers(headers.asJava)

    def headers(header: (String, String), rest: (String, String)*): PactDslRequestWithoutPath =
      this.headers(rest.toMap + header)

    def body[A](body: A)(implicit ev: PactBodyEncoder[A]): PactDslRequestWithoutPath =
      builder.body(ev.toJsonString(body))
    def body[A](body: A, mimeType: String)(implicit ev: PactBodyEncoder[A]): PactDslRequestWithoutPath =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType))
    def body[A](body: A, mimeType: String, charset: String)(implicit
        ev: PactBodyEncoder[A]
    ): PactDslRequestWithoutPath =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType, charset))

  }

  class PactDslResponseOps(val builder: PactDslResponse) extends AnyVal {
    def headers(headers: Map[String, String]): PactDslResponse = builder.headers(headers.asJava)

    def headers(header: (String, String), rest: (String, String)*): PactDslResponse =
      this.headers(rest.toMap + header)

    def body[A](body: A)(implicit ev: PactBodyEncoder[A]): PactDslResponse = builder.body(ev.toJsonString(body))
    def body[A](body: A, mimeType: String)(implicit ev: PactBodyEncoder[A]): PactDslResponse =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType))
    def body[A](body: A, mimeType: String, charset: String)(implicit ev: PactBodyEncoder[A]): PactDslResponse =
      builder.body(ev.toJsonString(body), ContentType.create(mimeType, charset))

  }
}

trait RequestResponsePactOps {
  implicit def toPactDslRequestWithPathOps(builder: PactDslRequestWithPath): PactDslRequestWithPathOps =
    new PactDslRequestWithPathOps(builder)
  implicit def toPactDslRequestWithoutPathOps(builder: PactDslRequestWithoutPath): PactDslRequestWithoutPathOps =
    new PactDslRequestWithoutPathOps(builder)
  implicit def toPactDslResponseOps(builder: PactDslResponse): PactDslResponseOps =
    new PactDslResponseOps(builder)
}
