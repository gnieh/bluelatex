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
package let

import compat.ProtocolTranslator

import common._
import http._
import permission._

import tiscaf._

import com.typesafe.config.Config

import net.liftweb.json.Serialization
import net.liftweb.json._

import scala.util.{Try, Failure, Success}

import gnieh.sohva.UserInfo
import gnieh.sohva.control.CouchClient

/** Legacy compatibility let, allowing mobwrite clients to synchronize papers
 *  with the new implementation.
 *  This let converts from the legacy protocol to the new one before sending the commands to the
 *  synchronization server, and convert it back to the old protocol before sending it back
 *  to the client.
 *
 *  It assumes that the client respects following conventions:
 *   - user name (sent via `F:`) must be of the form `<paper_id>/<file>`
 *
 *  @author Lucas Satabin
 */
class QLet(paperId: String, synchroServer: SynchroServer, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  override implicit val formats =
    BlueLet.formats +
    new SyncSessionSerializer +
    new SyncCommandSerializer +
    new SyncActionSerializer +
    new EditSerializer

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Any] = Try(permissions match {
    case Edit() =>
      // only authors may modify the paper content
      talk.req.octets match {
        case Some(octets) => {

          logInfo("mobwrite compatibility let called")

          val data = new String(octets, talk.req.contentEncoding)
          val talkValue = new StringBuilder
          // Convert mobwrite protocol to \BlueLaTeX's
          ProtocolTranslator.mobwrite2bluelatex(paperId, data).foreach(syncSession => {
            val sessionText = Serialization.write[SyncSession](syncSession)
            synchroServer.session(sessionText) match {
              case Success(result) => {
                val respSyncSession = Serialization.read[SyncSession](result)
                talkValue ++= ProtocolTranslator.bluelatex2mobwrite(respSyncSession)._2
              }
              case Failure(f) => logError(s"Could not process synchronization session for paper $paperId", f)
            }
          })
          // Write answer back to client
          if (!talkValue.isEmpty) {
            val bytes = talkValue.toString.getBytes(talk.encoding)
            talk
              .setContentLength(bytes.size)
              .write(bytes)
          } else {
            talk
              .setStatus(HStatus.InternalServerError)
              .writeJson(ErrorResponse("sync_error",
                                       s"Something went wrong when processing synchronization session for $paperId"))
          }
        }
        case None =>
          // nothing to do
          talk
            .setStatus(HStatus.NotModified)
            .writeJson(ErrorResponse("nothing_to_do", "No changes sent"))
      }
    case _ =>
      talk
        .setStatus(HStatus.Forbidden)
        .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to modify the paper content"))
  })

}

