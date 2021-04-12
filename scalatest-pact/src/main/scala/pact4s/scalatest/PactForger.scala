package pact4s.scalatest

import au.com.dius.pact.consumer.BaseMockServer
import org.scalatest.{Args, CompositeStatus, Status, Suite, SuiteMixin}
import pact4s.PactForgerResources

trait PactForger extends PactForgerResources with SuiteMixin { self: Suite =>

  def mockServer: BaseMockServer = server

  @volatile private var testFailed = false

  abstract override def run(testName: Option[String], args: Args): Status =
    if (expectedTestCount(args.filter) == 0) {
      new CompositeStatus(Set.empty)
    } else {
      validatePactVersion.map(throw _)
      server.start()
      server.waitForServer()
      try {
        val result = super.run(testName, args)
        if (!result.succeeds())
          testFailed = true
        result
      } finally {
        if (testFailed) {
          logger.info(
            s"Not writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to file because tests failed."
          )
        } else {
          logger.info(
            s"Writing pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}"
          )
          server.verifyResultAndWritePact(null, pactTestExecutionContext, pact, mockProviderConfig.getPactVersion)
        }
        server.stop()
      }
    }

}
