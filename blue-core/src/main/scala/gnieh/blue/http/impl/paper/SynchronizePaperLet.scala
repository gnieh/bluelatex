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
package impl
package paper

import com.typesafe.config.Config

import tiscaf._

import gnieh.sohva.UserInfo

import scala.io.Source

import scala.util.Try

/** A synchronization request for a paper. Only authors may send this kind of request
 *
 *  @author Lucas Satabin
 */
class SynchronizePaperLet(paperId: String, config: Config, synchroServer: SynchroServer) extends RoleLet(paperId, config) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Unit] = Try(role match {
    case Author =>
      // only authors may modify the paper content
      talk.req.octets match {
        case Some(octets) =>
          val data = new String(octets, talk.req.contentEncoding)
          val result = synchroServer.session(data)

          talk.write(result)

        case None =>
          // nothing to do
          talk.writeJson(ErrorResponse("nothing_to_do", "No changes sent"))
            .setStatus(HStatus.NotModified)
      }
    case _ =>
      talk.writeJson(ErrorResponse("no_sufficient_rights", "Only authors may modify the paper content"))
        .setStatus(HStatus.Forbidden)
  })

}
