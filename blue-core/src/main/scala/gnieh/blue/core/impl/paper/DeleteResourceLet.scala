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

import common._
import http._
import permission._

import tiscaf._

import resource._

import scala.util.Try

import gnieh.sohva.control.CouchClient

/** Deletes some resource associated to the paper.
 *
 *  @author Lucas Satabin
 */
class DeleteResourceLet(paperId: String, resourceName: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = Try(permissions match {
    case Edit() =>
      // only authors may get a resource
      val file = configuration.resource(paperId, resourceName)

      // delete the file
      val ok = file.delete

      // just say OK
      talk.writeJson(ok)

    case _ =>
      talk
        .setStatus(HStatus.Forbidden)
        .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to delete resources"))
  })

}

