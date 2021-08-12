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

package pact4s.weaver

import cats.data.NonEmptyList
import pact4s.PactVerifyResources
import sourcecode.{File, FileName, Line}
import weaver.{AssertionException, SourceLocation}

trait PactVerifier extends PactVerifyResources {
  override private[pact4s] def failure(message: String)(implicit fileName: FileName, file: File, line: Line): Nothing =
    throw AssertionException(message, NonEmptyList.of(SourceLocation(file.value, fileName.value, line.value)))
}
