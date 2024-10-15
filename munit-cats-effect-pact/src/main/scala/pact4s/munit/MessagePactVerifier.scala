package pact4s.munit

import munit.CatsEffectSuite

trait MessagePactVerifier extends PactVerifier { self: CatsEffectSuite =>
  def messages: ResponseFactory
  override def responseFactory: Option[ResponseFactory] = Some(messages)
}
