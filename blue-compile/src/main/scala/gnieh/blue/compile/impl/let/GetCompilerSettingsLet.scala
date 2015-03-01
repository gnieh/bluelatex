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
package compile
package impl
package let

import http._
import common._
import permission._

import tiscaf._

import com.typesafe.config.Config

import scala.util.{
  Try,
  Success,
  Failure
}

import gnieh.sohva.control.CouchClient

/** Handle request that want to access the compiler data
 *
 *  @author Lucas Satabin
 */
class GetCompilerSettingsLet(paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Any] = permissions match {
    case Configure() =>
      entityManager("blue_papers").getComponent[CompilerSettings](paperId) map {
        // we are sure that the settings has a revision because it comes from the database
        case Some(settings) =>
          talk.writeJson(settings, settings._rev.get)
        case None =>
          talk.setStatus(HStatus.NotFound).writeJson(ErrorResponse("not_found", s"No compiler for paper $paperId found"))
      } recover {
        case e =>
          logError(s"Error while retrieving compiler settings for paper $paperId", e)
          talk.setStatus(HStatus.InternalServerError).writeJson(ErrorResponse("cannot_get_compiler", s"No compiler for paper $paperId found"))
      }

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to see compiler settings")))

  }

}

