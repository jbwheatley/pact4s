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

sealed trait VerificationSettings

object VerificationSettings {

  /** For verifying message pacts, pact-jvm searches across the classpath for uniquely defined annotated methods that
    * represent the message produced by the provider. For example:
    * {{{
    *   @PactVerifyProvider("A message to say goodbye")
    *   def goodbyeMessage(): MessageAndMetadata = {
    *     val body = """{"goodbye":"harry"}"""
    *     MessageAndMetadataBuilder(body).build
    *   }
    * }}}
    *
    * @param packagesToScan
    *   which packages to scan, e.g. "pact4s.messages"
    */
  final case class AnnotatedMethodVerificationSettings(packagesToScan: List[String]) extends VerificationSettings
}
