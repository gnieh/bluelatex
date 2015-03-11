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

import http.{
  SyncPermissionLet,
  ErrorResponse
}

import common.{
  Logger,
  UserInfo
}

import permission._

import couch.{
  Paper,
  PaperRole
}

import com.typesafe.config.Config

import tiscaf.{
  HTalk,
  HStatus
}

import scala.io.Source

import scala.util.Try

import gnieh.sohva.control.CouchClient

/** Returns the paper roles
 *
 *  @author Lucas Satabin
 */
class GetPaperRolesLet(paperid: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperid, config, logger) {

  def permissionAct(user: Option[UserInfo], role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = permissions match {
    case Configure() =>
      val manager = entityManager("blue_papers")
      for(roles <- manager.getComponent[PaperRole](paperid))
        yield roles match {
          // we are sure that the paper has a revision because it comes from the database
          case Some(roles) =>
            talk.writeJson(Map("authors" -> roles.authors.users, "reviewers" -> roles.reviewers.users, "guests" -> roles.guests.users), roles._rev.get)
          case None =>
            talk.setStatus(HStatus.NotFound).writeJson(ErrorResponse("not_found", s"Paper $paperid not found"))
        }

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to see paper roles")))
  }

}
