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
package syntax

import scala.jdk.CollectionConverters._

object JavaHelpers {
  private[syntax] def convertJ(map: Map[String, Any]): java.util.Map[String, Any] =
    map.view.mapValues(convertValue).toMap.asJava

  private def convertValue(a: Any): Any = a match {
    case map: Map[_, _]  => map.view.mapValues(convertValue).asJava
    case as: Iterable[_] => as.map(convertValue).asJava
    case a               => a
  }
}
