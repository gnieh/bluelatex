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

/** A command sent for persisting a paper.
 *
 * @author Audric Schiltknecht
 */
case object PersistPaper

/** A command list sent for specific peer and file.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
final case class SyncSession(peerId: String,
                             paperId: String,
                             commands: List[Command])

/** A command to be performed during a Synchronization Session
 *
 *  @author Audric Schiltknecht
 */
sealed trait Command

/** Broadcast a message to all peers currently viewing the session paper.
 *
 *  @author Audric Schiltknecht
 */
final case class Message(from: String, json: JObject, retrieve: Boolean, filename: Option[String]) extends Command

/** A command to apply on a file from a given peer
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
final case class SyncCommand(filename: String, revision: Long, action: SyncAction) extends Command

/** An action to apply on a file from a given peer
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
sealed trait SyncAction

/** Request an edit to be made to the current session peer and file
 *  with given client (when sent by client)
 *  or server (when sent by server) revision.
 */
final case class Delta(revision: Long, data: List[Edit], overwrite: Boolean) extends SyncAction

/** Transmit the entire contents of the session file.
 */
final case class Raw(revision: Long, data: String, overwrite: Boolean) extends SyncAction

/** Delete the session file.
 */
case object Nullify extends SyncAction


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
