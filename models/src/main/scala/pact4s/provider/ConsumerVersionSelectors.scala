/*
 * Copyright 2021 io.github.jbwheatley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pact4s
package provider

import au.com.dius.pact.core.pactbroker.{ConsumerVersionSelectors => PactJVMSelectors}

import java.util
import scala.jdk.CollectionConverters._

/** Consumer version selectors. See https://docs.pact.io/pact_broker/advanced_topics/selectors
  */
final class ConsumerVersionSelectors private (selectors: Vector[PactJVMSelectors]) {
  private def add(selector: PactJVMSelectors) = new ConsumerVersionSelectors(selectors :+ selector)

  private[provider] def asJava: util.List[PactJVMSelectors] = selectors.asJava

  private[provider] def isEmpty: Boolean = selectors.isEmpty

  /** The latest version from the main branch of each consumer, as specified by the consumer's mainBranch property.
    */
  def mainBranch: ConsumerVersionSelectors = add(PactJVMSelectors.MainBranch.INSTANCE)

  /** The latest version from any branch of the consumer that has the same name as the current branch of the provider.
    * Used for coordinated development between consumer and provider teams using matching feature branch names.
    */
  def matchingBranch: ConsumerVersionSelectors = add(PactJVMSelectors.MatchingBranch.INSTANCE)

  /** The latest version from a particular branch of each consumer, or for a particular consumer if the second parameter
    * is provided. If fallback is provided, falling back to the fallback branch if none is found from the specified
    * branch.
    *
    * @param name
    *   - Branch name
    * @param consumer
    *   - Consumer name (optional)
    * @param fallback
    *   - Fall back to this branch if none is found from the specified branch (optional)
    */
  def branch(name: String, consumer: Option[String] = None, fallback: Option[String] = None): ConsumerVersionSelectors =
    add(
      new PactJVMSelectors.Branch(name, consumer.orNull, fallback.orNull)
    )

  /** All the currently deployed and currently released and supported versions of each consumer.
    */
  def deployedOrReleased: ConsumerVersionSelectors = add(PactJVMSelectors.DeployedOrReleased.INSTANCE)

  /** Any versions currently deployed to the specified environment
    */
  def deployedTo(environment: String): ConsumerVersionSelectors = add(new PactJVMSelectors.DeployedTo(environment))

  /** Any versions currently released and supported in the specified environment
    */
  def releasedTo(environment: String): ConsumerVersionSelectors = add(new PactJVMSelectors.ReleasedTo(environment))

  /** any versions currently deployed or released and supported in the specified environment
    */
  def environment(environment: String): ConsumerVersionSelectors = add(new PactJVMSelectors.Environment(environment))

  /** All versions with the specified tag
    */
  def tag(name: String): ConsumerVersionSelectors = add(new PactJVMSelectors.Tag(name))

  /** The latest version for each consumer with the specified tag
    */
  def latestTag(name: String): ConsumerVersionSelectors = add(new PactJVMSelectors.LatestTag(name))
}

object ConsumerVersionSelectors {
  def apply(): ConsumerVersionSelectors = new ConsumerVersionSelectors(Vector.empty)

  /** The latest version from the main branch of each consumer, as specified by the consumer's mainBranch property.
    */
  def mainBranch: ConsumerVersionSelectors = apply().mainBranch

  /** The latest version from any branch of the consumer that has the same name as the current branch of the provider.
    * Used for coordinated development between consumer and provider teams using matching feature branch names.
    */
  def matchingBranch: ConsumerVersionSelectors = apply().matchingBranch

  /** The latest version from a particular branch of each consumer, or for a particular consumer if the second parameter
    * is provided. If fallback is provided, falling back to the fallback branch if none is found from the specified
    * branch.
    *
    * @param name
    *   - Branch name
    * @param consumer
    *   - Consumer name (optional)
    * @param fallback
    *   - Fall back to this branch if none is found from the specified branch (optional)
    */
  def branch(name: String, consumer: Option[String] = None, fallback: Option[String] = None): ConsumerVersionSelectors =
    apply().branch(name, consumer, fallback)

  /** All the currently deployed and currently released and supported versions of each consumer.
    */
  def deployedOrReleased: ConsumerVersionSelectors = apply().deployedOrReleased

  /** Any versions currently deployed to the specified environment
    */
  def deployedTo(environment: String): ConsumerVersionSelectors = apply().deployedTo(environment)

  /** Any versions currently released and supported in the specified environment
    */
  def releasedTo(environment: String): ConsumerVersionSelectors = apply().releasedTo(environment)

  /** any versions currently deployed or released and supported in the specified environment
    */
  def environment(environment: String): ConsumerVersionSelectors = apply().environment(environment)

  /** All versions with the specified tag
    */
  def tag(name: String): ConsumerVersionSelectors = apply().tag(name)

  /** The latest version for each consumer with the specified tag
    */
  def latestTag(name: String): ConsumerVersionSelectors = apply().latestTag(name)
}
