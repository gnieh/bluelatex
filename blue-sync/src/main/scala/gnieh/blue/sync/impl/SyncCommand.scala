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
package sync
package impl

import net.liftweb.json._

/* Protocol adapted from Neil Fraser's mobwrite protocol:
 * http://code.google.com/p/google-mobwrite/wiki/Protocol
 */

/** A command list sent for specific user and file.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
final case class SyncSession(user: String,
                             file: String,
                             echo: Boolean,
                             revision: Long,
                             commands: List[SyncCommand])

/** A command to apply on a file from a given user
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
sealed trait SyncCommand

/** Request an edit to be made to the current session user and file
 *  with given client (when sent by client)
 *  or server (when sent by server) revision.
 */
final case class Delta(revision: Long, data: List[Edit], overwrite: Boolean) extends SyncCommand

/** Transmit the entire contents of the session file.
 */
final case class Raw(clientRevision: Long, data: String, overwrite: Boolean) extends SyncCommand

/** Delete the session file.
 */
case object Nullify extends SyncCommand

/** Broadcast a message to all users currently viewing the session paper.
*/
final case class Message(json: JObject, from: Option[String]) extends SyncCommand


/** Commands to edit file.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 *
 */
sealed trait Edit {
  val length: Int
}

object Edit {

  import EditCommandParsers._

  def unapply(s: String): Option[Edit] =
    parseAll(edit, s) match {
      case Success(value, _) => Some(value)
      case _ => None
    }
}

/** Keep `length` characters from current position.
 */
final case class Equality(length: Int) extends Edit {
  override def toString = s"=$length"
}
/** Delete `length` characters from the current position.
 */
final case class Delete(length: Int) extends Edit {
  override def toString = s"-$length"
}
/** Add the text at the current position.
 */
final case class Add(text: String) extends Edit {
  val length = text.length
  override def toString = s"+$text"
}
