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
package http
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

import gnieh.sohva.UserInfo

import resource._

import common._

import scala.util.Try

/** Backup the paper sources as a zip file
 *
 *  @author Lucas Satabin
 */
class BackupPaperLet(format: String, paperId: String, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Unit] = Try(role match {
    case Author =>
      // only authors may backup the paper sources
      import FileProcessing._
      val toZip =
        configuration.paperDir(paperId).filter(f => !f.isDirectory && !f.isHidden && !f.isTeXTemporary).toArray

      //logger.debug("Files to zip: " + toZip.mkString(", "))

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
                //logger.error("unable to add entry "
                //  + file.getName + " to zipn archive for paper "
                //  + paperId + ". Skipping it...", e)
            }
          }
        }

        zip.finish

        talk.setContentType(HMime.zip)
          .setContentLength(os.size)
          .write(os.toByteArray)
      }

    case _ =>
      talk
        .setStatus(HStatus.Forbidden)
        .writeJson(ErrorResponse("no_sufficient_rights", "Only authors may backup the paper sources"))
  })

}
