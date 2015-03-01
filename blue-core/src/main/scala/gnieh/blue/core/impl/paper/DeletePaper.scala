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

import couch._
import common._
import http._
import permission._

import java.util.UUID
import java.io.{
  File,
  FileWriter
}

import tiscaf._

import com.typesafe.config.Config

import org.osgi.framework.BundleContext

import resource._

import scala.sys.process._

import scala.util.{
  Try,
  Success
}

import gnieh.sohva.control.CouchClient

/** Delete an existing paper.
 *
 *  @author Lucas Satabin
 */
class DeletePaperLet(
  paperId: String,
  context: BundleContext,
  val couch: CouchClient,
  config: Config,
  recaptcha: ReCaptcha,
  logger: Logger)
    extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = permissions match {
    case Delete() =>
      // only authors may delete a paper
      // first delete the paper files
      import FileUtils._

      // delete the paper directory if it exists
      val paperDir = configuration.paperDir(paperId)
      val continue =
        if(paperDir.exists)
          paperDir.deleteRecursive()
        else
          true

      if(continue) {
        import OsgiUtils._

        val manager = entityManager("blue_papers")
        manager.deleteEntity(paperId) map {
          case true =>
            // notifiy deletion hooks
            for(hook <- context.getAll[PaperDeleted])
              Try(hook.afterDelete(paperId, entityManager("blue_papers")))
            talk.writeJson(true)
          case false =>
            talk
              .setStatus(HStatus.InternalServerError)
              .writeJson(ErrorResponse("cannot_delete_paper", "Unable to delete the paper database"))
        }

      } else {
        Success(talk
          .setStatus(HStatus.InternalServerError)
          .writeJson(ErrorResponse("cannot_delete_paper", "Unable to delete the paper files")))
      }

    case _ =>
      Success(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to delete this paper")))

  }

}

