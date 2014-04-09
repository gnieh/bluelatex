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
class DeletePaperLet(paperId: String, context: BundleContext, val couch: CouchClient, config: Config, recaptcha: ReCaptcha, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Unit] = role match {
    case Author =>
      // only authors may delete a paper
      // first delete the paper files
      import FileUtils._
      val dirDeleted = configuration.paperDir(paperId).deleteRecursive()
      if(dirDeleted) {
        import OsgiUtils._

        database("blue_papers").deleteDoc(paperId) map {
          case true =>
            // notifiy deletion hooks
            for(hook <- context.getAll[PaperDeleted])
              Try(hook.afterDelete(paperId, couchSession))
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
          .writeJson(ErrorResponse("no_sufficient_rights", "Only authors may delete a paper")))

  }

}

