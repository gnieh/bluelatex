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
package compat

import net.liftweb.json._

import scala.annotation.tailrec

import scala.util.Try

/** Converts protocols message from and back to mobwrite protocol
 *
 *  @author Lucas Satabin
 */
object ProtocolTranslator {

  private implicit class ExtractorContext(val sc: StringContext) {
    object re {
      val regex = sc.parts.mkString("([^:]+|.*)").r
      def unapplySeq(s: String): Option[Seq[String]] =
        regex.unapplySeq(s)
    }
  }

  private object name {
    def unapply(str: String): Option[String] =
      if(str.matches("[a-zA-Z][a-zA-Z0-9_:.-]*"))
        Some(str)
      else
        None
  }

  private object long {
    def unapply(str: String): Option[Long] =
      Try(str.toLong).toOption
  }

  private object obj {
    def unapply(str: String): Option[JObject] =
      parseOpt(str).collect { case obj @ JObject(_) => obj }
  }

  def mobwrite2bluelatex(paperId: String, commands: String): List[SyncSession] = {
    val result = new StringBuilder
    @tailrec
    def translate(commands: List[String],
                  currentPeer: Option[String],
                  currentFile: Option[String],
                  currentRevision: Option[Long],
                  commandsAcc: List[Command],
                  sessionsAcc: List[SyncSession]): List[SyncSession] = commands match {

      case re"(?:u|U):${name(peerId)}" :: rest =>
        val sessions = currentPeer match {
          case Some(peer) if commandsAcc.nonEmpty =>
            SyncSession(peer, paperId, commandsAcc.reverse) :: sessionsAcc
          case _ =>
            sessionsAcc
        }
        translate(rest, Some(peerId), currentFile, currentRevision, Nil, sessions)

      case re"(?:f|F):${long(rev)}:${name(file)}" :: rest =>
        translate(rest, currentPeer, Some(file), Some(rev), commandsAcc, sessionsAcc)

      case re"(?:f|F):${name(file)}" :: rest =>
        translate(rest, currentPeer, Some(file), None, commandsAcc, sessionsAcc)

      case re"d:${long(rev)}:$delta" :: rest =>
        val commands = (currentFile, currentRevision) match {
          case (Some(file), Some(frev)) =>
            val edits = EditCommandParsers.parseEdits(delta)
            SyncCommand(file, frev, Delta(rev, edits, false)) :: commandsAcc
          case (Some(file), None) =>
            val edits = EditCommandParsers.parseEdits(delta)
            SyncCommand(file, -1, Delta(rev, edits, false)) :: commandsAcc
          case _ =>
            commandsAcc
        }
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"D:${long(rev)}:$delta" :: rest =>
        val commands = (currentFile, currentRevision) match {
          case (Some(file), Some(frev)) =>
            val edits = EditCommandParsers.parseEdits(delta)
            SyncCommand(file, frev, Delta(rev, edits, true)) :: commandsAcc
          case (Some(file), None) =>
            val edits = EditCommandParsers.parseEdits(delta)
            SyncCommand(file, -1, Delta(rev, edits, true)) :: commandsAcc
          case _ =>
            commandsAcc
        }
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"r:${long(rev)}:$content" :: rest =>
        val commands = (currentFile, currentRevision) match {
          case (Some(file), Some(frev)) =>
            SyncCommand(file, frev, Raw(rev, content, false)) :: commandsAcc
          case (Some(file), None) =>
            SyncCommand(file, -1, Raw(rev, content, false)) :: commandsAcc
          case _ =>
            commandsAcc
        }
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"R:${long(rev)}:$content" :: rest =>
        val commands = (currentFile, currentRevision) match {
          case (Some(file), Some(frev)) =>
            SyncCommand(file, frev, Raw(rev, content, true)) :: commandsAcc
          case (Some(file), None) =>
            SyncCommand(file, -1, Raw(rev, content, true)) :: commandsAcc
          case _ =>
            commandsAcc
        }
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"(?:n|N):${name(file)}" :: rest =>
        val commands = SyncCommand(file, -1, Nullify) :: commandsAcc
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"(?:m|M):${obj(content)}" :: rest =>
        val commands = Message(currentPeer.getOrElse(""), content, true, currentFile) :: commandsAcc
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case _ :: rest =>
        // simply ignore unknown commands
        translate(rest, currentPeer, currentFile, currentRevision, commandsAcc, sessionsAcc)

      case Nil =>
        val sessions = currentPeer match {
          case Some(peer) if commandsAcc.nonEmpty =>
            SyncSession(peer, paperId, commandsAcc.reverse) :: sessionsAcc
          case _ =>
            sessionsAcc
        }
        sessions.reverse

    }
    translate(commands.split("\n").toList, None, None, None, Nil, Nil)
  }

  def bluelatex2mobwrite(session: SyncSession): (String, String) = {
    val SyncSession(peerId, paperId, commands) = session
    val result = new StringBuilder
    for(command <- commands)
      command match {
        case SyncCommand(filename, lastRevision, action) =>
          action match {
            case Delta(newRevision, data, true) =>
              result ++= s"""U:$peerId
                            |F:$lastRevision:$filename
                            |D:$newRevision:${data.mkString("\t")}
                            |""".stripMargin
            case Delta(newRevision, data, false) =>
              result ++= s"""U:$peerId
                            |F:$lastRevision:$filename
                            |d:$newRevision:${data.mkString("\t")}
                            |""".stripMargin
            case Raw(newRevision, data, true) =>
              result ++= s"""U:$peerId
                            |F:$lastRevision:$filename
                            |R:$data
                            |""".stripMargin
            case Raw(newRevision, data, false) =>
              result ++= s"""U:$peerId
                            |F:$lastRevision:$filename
                            |r:$data
                            |""".stripMargin
            case Nullify =>
              result ++= s"""U:$peerId
                            |N:$filename
                            |""".stripMargin
          }
        case Message(peerId, obj, reply, Some(file)) =>
          result ++= s"""U:$peerId
                        |F:$file
                        |M:${compact(render(obj))}
                        |""".stripMargin
        case Message(peerId, obj, reply, None) =>
          result ++= s"""U:$peerId
                        |M:${compact(render(obj))}
                        |""".stripMargin
      }
    (paperId, result.toString)
  }

}

