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

import http._
import common._
import permission._

import scala.io.Source

import scala.util.Try

import gnieh.sohva.control.CouchClient

/** Gives access to the non-synchronized resource list for the given paper.
 *
 *  @author Lucas Satabin
 */
class NonSynchronizedResourcesLet(paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = Try(permissions match {
    case Edit() =>
      import FileUtils._
      val files =
        configuration
          .paperDir(paperId)
          .filter(f =>
              !f.extension.matches(synchronizedExt)
              && !f.extension.matches(generatedExt)
              && !f.isHidden
              && !f.isDirectory
          )
          .map(_.getName)
      talk.writeJson(files)
    case _ =>
      talk
        .setStatus(HStatus.Forbidden)
        .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to see the list of non synchronized resources"))
  })

}

