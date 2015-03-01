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

import http._
import common._
import permission._

import com.typesafe.config.Config

import akka.actor.ActorSystem

import tiscaf._

import scala.util.Try

import gnieh.sohva.control.CouchClient

/** Notify the system that the user joined a given paper
 *
 *  @author Lucas Satabin
 */
class JoinPaperLet(
  paperId: String,
  peerId: String,
  system: ActorSystem,
  val couch: CouchClient,
  config: Config,
  logger: Logger)
    extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = Try {
    permissions match {
      case Edit() | Read() =>
        system.eventStream.publish(Join(peerId, paperId))
        talk.writeJson(true)
      case _ =>
        talk
          .setStatus(HStatus.Unauthorized)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to join the paper"))
    }
  }

}

