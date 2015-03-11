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

import java.util.zip.{
  ZipOutputStream,
  ZipEntry,
  ZipException
}
import java.io.{
  FileInputStream,
  ByteArrayOutputStream
}

import tiscaf._

import resource._

import http._
import common._
import permission._

import couch.Paper

import scala.util.Try

import gnieh.sohva.control.CouchClient

/** Backup the paper sources as a zip file
 *
 *  @author Lucas Satabin
 */
class BackupPaperLet(format: String, paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: Option[UserInfo], role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = permissions match {
    case Download() =>
      entityManager("blue_papers").getComponent[Paper](paperId) map {
        case Some(Paper(_, name, _)) =>
          // only authors may backup the paper sources
          import FileUtils._
          val toZip =
            configuration.paperDir(paperId).filter(f => !f.isDirectory && !f.isHidden && !f.isTeXTemporary).toArray

          for(os <- managed(new ByteArrayOutputStream);
              zip <- managed(new ZipOutputStream(os))) {
            for (file <- toZip) {
              for(fis <- managed(new FileInputStream(file))) {
                try {
                  // create a new entry
                  zip.putNextEntry(new ZipEntry(file.getName))
                  // write into this entry
                  val length = fis.available
                  for (i <- 0 until length)
                    zip.write(fis.read)
                  // close the entry which was just written
                  zip.closeEntry
                } catch {
                  case e: ZipException =>
                }
              }
            }

            zip.finish

            talk.setContentType(HMime.zip)
              .setContentLength(os.size)
              .setFilename(s"$name.$format")
              .write(os.toByteArray)
          }

        case None =>

          talk
            .setStatus(HStatus.NotFound)
            .writeJson(ErrorResponse("not_found", s"No paper data for paper $paperId"))
      }

    case _ =>
      Try(talk
        .setStatus(HStatus.Forbidden)
        .writeJson(ErrorResponse("no_sufficient_rights", "You have no right to download the paper sources")))
  }

}
