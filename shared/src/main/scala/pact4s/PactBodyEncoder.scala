package pact4s

trait PactBodyEncoder[A] {
  def toJsonString(a: A): String
}
