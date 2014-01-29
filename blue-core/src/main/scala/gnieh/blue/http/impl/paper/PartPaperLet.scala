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

import common._

import com.typesafe.config.Config

import akka.actor.ActorSystem

import tiscaf._

import scala.util.Try

/** Notify the system that the user left a given paper
 *
 *  @author Lucas Satabin
 */
class PartPaperLet(paperId: String, system: ActorSystem, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Unit] = role match {
    case Author | Reviewer =>
      Try(system.eventStream.publish(Part(user.name, Some(paperId))))
    case _ =>
      Try(
        talk
          .setStatus(HStatus.Unauthorized)
          .writeJson(ErrorResponse("no_sufficient_rights", "Only authors and reviewers may leave a paper"))
      )
  }

}

