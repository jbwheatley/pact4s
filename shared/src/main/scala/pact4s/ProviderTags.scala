/*
 * Copyright 2021-2021 io.github.jbwheatley
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

final case class ProviderTags(head: String, tail: List[String]) {
  def ::(tag: String): ProviderTags        = ProviderTags(tag, head :: tail)
  private[pact4s] def toList: List[String] = head :: tail
}

object ProviderTags {
  def apply(tag: String, rest: String*): ProviderTags = ProviderTags(tag, rest: _*)

  def fromList(tags: List[String]): Option[ProviderTags] = tags match {
    case head :: tail => Some(ProviderTags(head, tail))
    case Nil          => None
  }
}
