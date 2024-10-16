package pact4s
package syntax

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import pact4s.syntax.PactDslPartOps.PactDslJsonBodyOps

object PactDslPartOps {
  class PactDslJsonBodyOps(val builder: PactDslJsonBody) extends AnyVal {
    def intType(name: String, value: Int): PactDslJsonBody   = builder.integerType(name, value: Integer)
    def longType(name: String, value: Long): PactDslJsonBody = builder.integerType(name, value: java.lang.Long)
  }
}

trait PactDslPartOps {
  implicit def toPactDslJsonBodyOps(builder: PactDslJsonBody): PactDslJsonBodyOps =
    new PactDslJsonBodyOps(builder)
}
