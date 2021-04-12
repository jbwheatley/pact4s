package pact4s.sbt

import pact4s.sbt.PactBrokerAuth.NoAuth

import scala.util.matching.Regex

final case class PactPublishSettings private[sbt](
                                                 pactDirectory: String,
    pactBrokerUrl: String,
                                                 tags: List[String] = Nil,
                                                 pactBrokerAuth: PactBrokerAuth = NoAuth,
                                                 excludes: List[Regex] = Nil,
                                           )

sealed trait PactBrokerAuth

object PactBrokerAuth {
  case object NoAuth extends PactBrokerAuth
  final case class BasicAuth(username: String, password: String) extends PactBrokerAuth
  final case class BearerTokenAuth(token: String) extends PactBrokerAuth
}

object PactPublishSettings {
  def apply(): PactPublishSettings = PactPublishSettings("", "")
}