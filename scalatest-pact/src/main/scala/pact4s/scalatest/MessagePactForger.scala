package pact4s.scalatest

import au.com.dius.pact.core.model.messaging.Message
import org.scalatest._
import pact4s.MessagePactForgerResources

import scala.jdk.CollectionConverters.ListHasAsScala

trait MessagePactForger extends MessagePactForgerResources with SuiteMixin { self: Suite =>

  val messages: List[Message] = pact.getMessages.asScala.toList

  @volatile private var testFailed = false

  abstract override def run(testName: Option[String], args: Args): Status =
    if (expectedTestCount(args.filter) == 0) {
      new CompositeStatus(Set.empty)
    } else {
      validatePactVersion(pactSpecVersion).foreach(throw _)
      try {
        val result = super.run(testName, args)
        if (!result.succeeds())
          testFailed = true
        result
      } finally if (testFailed) {
        logger.info(
          s"Not writing message pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to file because tests failed."
        )
      } else {
        logger.info(
          s"Writing message pacts for consumer ${pact.getConsumer} and provider ${pact.getProvider} to ${pactTestExecutionContext.getPactFolder}"
        )
        val write = pact.write(pactTestExecutionContext.getPactFolder, pactSpecVersion)
        Option(write.component2()).foreach(throw _)
      }
    }

}
