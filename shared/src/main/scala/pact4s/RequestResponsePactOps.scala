package pact4s

import au.com.dius.pact.consumer.dsl.{PactDslRequestWithPath, PactDslRequestWithoutPath, PactDslResponse}

import scala.jdk.CollectionConverters.MapHasAsJava
import RequestResponsePactOps._
import org.apache.http.entity.ContentType

object RequestResponsePactOps {
  final class PactDslRequestWithPathOps(builder: PactDslRequestWithPath) {
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

  final class PactDslRequestWithoutPathOps(builder: PactDslRequestWithoutPath) {
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

  final class PactDslResponseOps(builder: PactDslResponse) {
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
