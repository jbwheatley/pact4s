package pact4s

import au.com.dius.pact.core.model.messaging.Message

trait MessagePactDecoder[A] {
  def decode(message: Message): Either[Throwable, A]
}
