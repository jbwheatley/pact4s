package pact4s.circe

import io.circe.Encoder
import pact4s.PactBodyEncoder

object implicits {
  implicit def pactBodyEncoder[A](implicit encoder: Encoder[A]): PactBodyEncoder[A] =
    (a: A) => encoder(a).noSpaces
}
