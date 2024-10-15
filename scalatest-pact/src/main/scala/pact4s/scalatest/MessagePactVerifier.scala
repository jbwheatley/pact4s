package pact4s.scalatest

import org.scalatest.Suite

trait MessagePactVerifier extends PactVerifier { self: Suite =>
  def messages: ResponseFactory
  override def responseFactory: Option[ResponseFactory] = Some(messages)
}
