package pact4s.vulcan

//import au.com.dius.pact.consumer.dsl.{DslPart, PactDslJsonBody}
import au.com.dius.pact.core.model.messaging.Message
//import au.com.dius.pact.core.support.json.JsonValue
import cats.implicits._
//import io.circe.Decoder.Result
//import io.circe.parser._
//import io.circe.syntax._
//import io.circe.{Codec, Decoder, Encoder, HCursor, Json}
//import pact4s.circe.JsonConversion.jsonToPactDslJsonBody
import pact4s.{MessagePactDecoder, PactBodyJsonEncoder}
import vulcan._

object implicits {
  private def toException(avroError: AvroError): Exception =
    new Exception(avroError.message, avroError.throwable)

  private def printLong(ex: Throwable): Unit = {
    ex.printStackTrace()
    Option(ex.getCause).foreach(printLong)
  }

  implicit def pactBodyEncoder[A](implicit codec: Codec[A]): PactBodyJsonEncoder[A] =
    Codec.toJson(_).valueOr { avroError =>
      val ex = toException(avroError)
      printLong(ex)
      throw ex
    }

//  implicit def pactDslJsonBodyConverter[A](implicit codec: Codec[A]): PactDslJsonBodyEncoder[A] =
//    new PactDslJsonBodyEncoder[A] {
//      override def toPactDslJsonBody(a: A): DslPart =
//        new PactDslJsonBody().setBody(JsonValue.Object)
//    }
  //jsonToPactDslJsonBody(codec.encode(a))

  implicit def messagePactDecoder[A](implicit codec: Codec[A]): MessagePactDecoder[A] =
    (message: Message) => Codec.decode[A](message.contentsAsString()).leftMap(toException)

//  implicit val providerStateCodec: Codec[ProviderState] = new Codec[ProviderState] {
//    def apply(a: ProviderState): Json            = Json.obj("state" -> a.state.asJson)
//    def apply(c: HCursor): Result[ProviderState] = c.get[String]("state").map(ProviderState)
//  }
}
