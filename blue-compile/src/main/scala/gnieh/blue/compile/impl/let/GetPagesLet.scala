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

import org.apache.pdfbox.pdmodel.PDDocument

import resource._

import gnieh.sohva.control.CouchClient

/** Handle request that ask for the number of pages in the compiled paper.
 *
 *  @author Lucas Satabin
 */
class GetPagesLet(paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  import FileUtils._

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Any] = permissions match {
    case Read() =>

      // the generated pdf file
      val pdfFile = configuration.buildDir(paperId) / s"main.pdf"

      if(pdfFile.exists) {

          managed(PDDocument.load(pdfFile)).map(_.getNumberOfPages).either match {
            case Right(pages) =>
              Try(talk.writeJson(pages))
            case Left(errors) =>
              logError(s"Cannot extract number of pages for paper $paperId", errors.head)
              Try(
                talk
                  .setStatus(HStatus.InternalServerError)
                  .writeJson(ErrorResponse("unknown_error", "The number of pages could not be extracted")))
          }

      } else {
        Try(
          talk
            .setStatus(HStatus.NotFound)
            .writeJson(ErrorResponse("not_found", "No compiled version of paper $paperId found")))
      }

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to see the number of pages")))

  }

}

