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

package pact4s.ziotest

import pact4s.PactVerifyResources
import pact4s.effect.{Id, MonadLike}
import sourcecode.{File, FileName, Line}

trait PactVerifier extends PactVerifyResources[Id] {
  override private[pact4s] def skip(message: String)(implicit fileName: FileName, file: File, line: Line): Unit = ()

  override private[pact4s] def failure(message: String)(implicit fileName: FileName, file: File, line: Line): Nothing =
    throw new RuntimeException(message)

  override private[pact4s] implicit val F: MonadLike[Id] = MonadLike.idMonadLike

}

trait MessagePactVerifier extends PactVerifier {
  def messages: ResponseFactory
  override def responseFactory: Option[ResponseFactory] = Some(messages)
}
