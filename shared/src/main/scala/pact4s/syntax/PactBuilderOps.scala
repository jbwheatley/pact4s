package pact4s.syntax

import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import pact4s.syntax.PactOps.PactBuilderOps

object PactOps {
  class PactBuilderOps(val builder: PactBuilder) extends AnyVal {
    def consumer(consumer: String): PactBuilder = {
      builder.setConsumer(consumer)
      builder
    }

    def provider(provider: String): PactBuilder = {
      builder.setProvider(provider)
      builder
    }

    def pactVersion(pactVersion: PactSpecVersion): PactBuilder = {
      builder.setPactVersion(pactVersion)
      builder
    }
  }
}

trait PactOps {
  implicit def toPactBuilderOps(builder: PactBuilder): PactBuilderOps = new PactBuilderOps(builder)
}
