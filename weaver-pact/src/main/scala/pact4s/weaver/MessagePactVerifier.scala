package pact4s.weaver

trait MessagePactVerifier[F[+_]] extends PactVerifier[F] {
  def messages: ResponseFactory
  override def responseFactory: Option[ResponseFactory] = Some(messages)
}
