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
package provider

import java.time.{Instant, LocalDate, OffsetDateTime, ZoneOffset}

sealed abstract case class WipPactsSince(since: Option[Instant])

object WipPactsSince {
  val never: WipPactsSince = new WipPactsSince(None) {}

  def localDate(since: LocalDate): WipPactsSince = maybeLocalDate(Some(since))
  def maybeLocalDate(since: Option[LocalDate]): WipPactsSince = new WipPactsSince(
    since.map(_.atStartOfDay().toInstant(ZoneOffset.UTC))
  ) {}

  def instant(since: Instant): WipPactsSince              = maybeInstant(Some(since))
  def maybeInstant(since: Option[Instant]): WipPactsSince = new WipPactsSince(since) {}

  def offsetDateTime(since: OffsetDateTime): WipPactsSince = maybeOffsetDateTime(Some(since))
  def maybeOffsetDateTime(since: Option[OffsetDateTime]): WipPactsSince = new WipPactsSince(
    since.map(_.toInstant)
  ) {}
}
