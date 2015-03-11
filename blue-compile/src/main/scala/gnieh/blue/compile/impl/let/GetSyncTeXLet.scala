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

class GetSyncTeXLet(paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  import FileUtils._

  def permissionAct(user: Option[UserInfo], role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Any] = permissions match {
    case Read() =>
      val syncTeXFile = configuration.buildDir(paperId) / s"main.synctex.gz"

      if (!syncTeXFile.exists)
        Try(
          talk
            .setStatus(HStatus.NotFound)
            .writeJson(ErrorResponse("not_found", "SyncTeX file not found")))
      else
        Try(for(is <- managed(new FileInputStream(syncTeXFile))) {
          val array =
            Iterator.continually(is.read).takeWhile(_ != -1). map(_.toByte).toArray

          talk
            .setHeader("Content-Encoding", "gzip")
            .setContentType(HMime.txt)
            .setContentLength(array.length)
            .write(array)
        })

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to retrieve SyncTeX data")))

  }

}

