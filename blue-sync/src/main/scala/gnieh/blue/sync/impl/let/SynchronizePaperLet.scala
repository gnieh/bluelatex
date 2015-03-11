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

import common._
import http._
import permission._

import com.typesafe.config.Config

import tiscaf._

import scala.io.Source

import scala.util.{Try, Success, Failure}

import gnieh.sohva.control.CouchClient

/** A synchronization request for a paper. Only authors may send this kind of request
 *
 *  @author Lucas Satabin
 */
class SynchronizePaperLet(paperId: String, synchroServer: SynchroServer, val couch: CouchClient, config: Config, logger: Logger)
    extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: Option[UserInfo], role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = Try(permissions match {
    case Edit() =>
      // only authors may modify the paper content
      talk.req.octets match {
        case Some(octets) =>
          val data = new String(octets, talk.req.contentEncoding)
          synchroServer.session(data) match {
            case Success(result) =>
              // TODO use `writeJson` once the synchronization server returns
              // a SyncSession instead of a string
              val bytes = result.getBytes(talk.encoding)
              talk
                .setContentType(s"${HMime.json};charset=${talk.encoding}")
                .setContentLength(bytes.size)
                .write(bytes)
            case Failure(f) => {
              logError(s"Could not process synchronization session for paper $paperId", f)
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
        .writeJson(ErrorResponse("no_sufficient_rights", "You have not permission to modify the paper content"))
  })

}
