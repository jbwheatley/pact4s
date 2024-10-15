package http
package consumer

import au.com.dius.pact.consumer.PactTestExecutionContext
import cats.effect.IO
import cats.effect.unsafe.implicits._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

import java.util.Base64

trait ExamplePactCommons {
  /*
  we can define the folder that the pact contracts get written to upon completion of this test suite.
   */
  val executionContext: PactTestExecutionContext = new PactTestExecutionContext(
    "./example/resources/pacts"
  )

  protected val testID           = "testID"
  protected val missingID        = "missingID"
  protected val newResource      = Resource("newID", 234)
  protected val conflictResource = Resource("conflict", 234)

  protected def mkAuthHeader(pass: String) = s"Basic ${Base64.getEncoder.encodeToString(s"user:$pass".getBytes)}"

  lazy val client: Client[IO] = EmberClientBuilder.default[IO].build.allocated.unsafeRunSync()._1
}
