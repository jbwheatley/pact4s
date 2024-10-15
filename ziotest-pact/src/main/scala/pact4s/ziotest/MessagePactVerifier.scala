package pact4s.ziotest

trait MessagePactVerifier extends PactVerifier {
  def messages: ResponseFactory
  override def responseFactory: Option[ResponseFactory] = Some(messages)
}
