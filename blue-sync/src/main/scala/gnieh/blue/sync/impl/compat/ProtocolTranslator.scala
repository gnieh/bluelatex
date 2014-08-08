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

import java.net.{URLDecoder, URLEncoder}

/** Converts protocols message from and back to mobwrite protocol
 *
 *  @author Lucas Satabin
 */
object ProtocolTranslator {

  private implicit class ExtractorContext(val sc: StringContext) {
    def re = sc.parts.mkString.r
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

      case re"(?:u|U):(.*)${name(peerId)}" :: rest =>
        val sessions = currentPeer match {
          case Some(peer) if commandsAcc.nonEmpty =>
            SyncSession(peer, paperId, commandsAcc.reverse) :: sessionsAcc
          case _ =>
            sessionsAcc
        }
        translate(rest, Some(peerId), currentFile, currentRevision, Nil, sessions)

      case re"(?:f|F):(\d+)${long(rev)}:(.*)${name(file)}" :: rest =>
        translate(rest, currentPeer, Some(file), Some(rev), commandsAcc, sessionsAcc)

      case re"(?:f|F):(.*)${name(file)}" :: rest =>
        translate(rest, currentPeer, Some(file), None, commandsAcc, sessionsAcc)

      case re"d:(\d+)${long(rev)}:(.*)$delta" :: rest =>
        val commands = (currentFile, currentRevision) match {
          case (Some(file), Some(frev)) =>
            val editStrings = delta.split('\t').toList
            val edits = editStrings map { s => EditCommandParsers.parseEdit(decodeUriCompatibility(s)) }
            SyncCommand(file, frev, Delta(rev, edits.flatten, false)) :: commandsAcc
          case (Some(file), None) =>
            val editStrings = delta.split('\t').toList
            val edits = editStrings map { s => EditCommandParsers.parseEdit(decodeUriCompatibility(s)) }
            SyncCommand(file, -1, Delta(rev, edits.flatten, false)) :: commandsAcc
          case _ =>
            commandsAcc
        }
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"D:(\d+)${long(rev)}:(.*)$delta" :: rest =>
        val commands = (currentFile, currentRevision) match {
          case (Some(file), Some(frev)) =>
            val editStrings = delta.split('\t').toList
            val edits = editStrings map { s => EditCommandParsers.parseEdit(decodeUriCompatibility(s)) }
            SyncCommand(file, frev, Delta(rev, edits.flatten, true)) :: commandsAcc
          case (Some(file), None) =>
            val editStrings = delta.split('\t').toList
            val edits = editStrings map { s => EditCommandParsers.parseEdit(decodeUriCompatibility(s)) }
            SyncCommand(file, -1, Delta(rev, edits.flatten, true)) :: commandsAcc
          case _ =>
            commandsAcc
        }
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"r:(\d+)${long(rev)}:(.*)$content" :: rest =>
        val commands = (currentFile, currentRevision) match {
          case (Some(file), Some(frev)) =>
            SyncCommand(file, frev, Raw(rev, decodeUriCompatibility(content), false)) :: commandsAcc
          case (Some(file), None) =>
            SyncCommand(file, -1, Raw(rev, decodeUriCompatibility(content), false)) :: commandsAcc
          case _ =>
            commandsAcc
        }
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"R:(\d+)${long(rev)}:(.*)$content" :: rest =>
        val commands = (currentFile, currentRevision) match {
          case (Some(file), Some(frev)) =>
            SyncCommand(file, frev, Raw(rev, decodeUriCompatibility(content), true)) :: commandsAcc
          case (Some(file), None) =>
            SyncCommand(file, -1, Raw(rev, decodeUriCompatibility(content), true)) :: commandsAcc
          case _ =>
            commandsAcc
        }
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"(?:n|N):(.*)${name(file)}" :: rest =>
        val commands = SyncCommand(file, -1, Nullify) :: commandsAcc
        translate(rest, currentPeer, currentFile, currentRevision, commands, sessionsAcc)

      case re"(?:m|M):(.*)${obj(content)}" :: rest =>
        val commands = Message(currentPeer.getOrElse(""), content, currentFile) :: commandsAcc
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
                            |D:$newRevision:${data.map{d => encodeUriCompatibility(d.toString)}.mkString("\t")}
                            |""".stripMargin
            case Delta(newRevision, data, false) =>
              result ++= s"""U:$peerId
                            |F:$lastRevision:$filename
                            |d:$newRevision:${data.map{d => encodeUriCompatibility(d.toString)}.mkString("\t")}
                            |""".stripMargin
            case Raw(newRevision, data, true) =>
              result ++= s"""U:$peerId
                            |F:$lastRevision:$filename
                            |R:$newRevision:${encodeUriCompatibility(data)}
                            |""".stripMargin
            case Raw(newRevision, data, false) =>
              result ++= s"""U:$peerId
                            |F:$lastRevision:$filename
                            |r:$newRevision:${encodeUriCompatibility(data)}
                            |""".stripMargin
            case Nullify =>
              result ++= s"""U:$peerId
                            |N:$filename
                            |""".stripMargin
          }
        case Message(peerId, obj, Some(file)) =>
          result ++= s"""U:$peerId
                        |F:$file
                        |M:${compact(render(obj))}
                        |""".stripMargin
        case Message(peerId, obj, None) =>
          result ++= s"""U:$peerId
                        |M:${compact(render(obj))}
                        |""".stripMargin
      }
    (paperId, result.toString)
  }

  /**
   * Encode selected chars for compatibility with JavaScript's encodeURI.
   * Taken from "Diff Match Patch" java's implementation by Neil Fraser.
   * @param str The string to encode.
   * @return The encoded string.
   */
  private def encodeUriCompatibility(str: String): String = {
    return URLEncoder.encode(str, "UTF-8")
        .replace('+', ' ').replace("%21", "!").replace("%7E", "~")
        .replace("%27", "'").replace("%28", "(").replace("%29", ")")
        .replace("%3B", ";").replace("%2F", "/").replace("%3F", "?")
        .replace("%3A", ":").replace("%40", "@").replace("%26", "&")
        .replace("%3D", "=").replace("%2B", "+").replace("%24", "$")
        .replace("%2C", ",").replace("%23", "#")
  }


  /**
   * Decode selected chars for compatibility with JavaScript's decodeURI.
   * Taken from "Diff Match Patch" java's implementation by Neil Fraser.
   * @param str The string to decode.
   * @return The decoded string, empty string if `str` is invalid or system does not support UTF-8
   */
  private def decodeUriCompatibility(str: String): String = {
    // decode would change all "+" to " ", and we need to keep them (for Add)
    val formattedString = str.replace("+", "%2B")
    Try {
      URLDecoder.decode(formattedString, "UTF-8")
    }.getOrElse("")
  }

}

