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
package core
package impl
package paper

import com.typesafe.config.Config

import tiscaf._

import common._
import http._

import scala.io.Source

import scala.util.{Try, Success, Failure}

/** A synchronization request for a paper. Only authors may send this kind of request
 *
 *  @author Lucas Satabin
 */
class SynchronizePaperLet(paperId: String, config: Config, synchroServer: SynchroServer, logger: Logger)
    extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Unit] = Try(role match {
    case Author =>
      // only authors may modify the paper content
      talk.req.octets match {
        case Some(octets) =>
          val data = new String(octets, talk.req.contentEncoding)
          synchroServer.session(data) match {
            case Success(result) => talk.write(result)
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
        .writeJson(ErrorResponse("no_sufficient_rights", "Only authors may modify the paper content"))
  })

}
