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

import scala.util.Try

import java.io.FileInputStream

import gnieh.sohva.control.CouchClient

import resource._

class GetPdfLet(paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: Role)(implicit talk: HTalk): Try[Any] = role match {
    case Author | Reviewer =>

      import FileUtils._

      val pdfFile = configuration.buildDir(paperId) / s"$paperId.pdf"

      if(pdfFile.exists)
        Try(for(pdf <- managed(new FileInputStream(pdfFile))) {
          val array =
            Iterator.continually(pdf.read).takeWhile(_ != -1).map(_.toByte).toArray

          talk.setContentType(HMime.pdf)
            .setContentLength(array.length)
            .setFilename(pdfFile.getName)
            .write(array)
        })
      else
        Try(
          talk
            .setStatus(HStatus.NotFound)
            .writeJson(ErrorResponse("not_found", "compiled paper $paperId not found")))

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "Only authors and reviewers may see compiled paper")))

  }

}

