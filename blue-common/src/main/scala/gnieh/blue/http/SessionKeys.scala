/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gnieh.blue
package http

import tiscaf.HTalk

/** Keys of object stored in the session
 *
 *  @author Lucas Satabin
 *
 */
object SessionKeys {

  val Username = "username"

  val Peers = "peers"

  val Couch = "couch"

  val Git = "git"

  /** Gets the value of the given type in the session if it exists */
  def get[T: Manifest](key: String)(implicit talk: HTalk): Option[T] =
    talk.ses.get(key).collect { case v: T => v }

}

