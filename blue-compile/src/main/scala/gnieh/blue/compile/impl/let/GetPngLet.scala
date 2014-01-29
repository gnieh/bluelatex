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

import tiscaf._

// image generation
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFImageWriter
import java.awt.image.BufferedImage

import com.typesafe.config.Config

import scala.util.Try

import java.io.FileInputStream


import resource._

class GetPngLet(paperId: String, page: Int, density: Int, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  import FileProcessing._

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Any] = role match {
    case Author | Reviewer =>
      val pngPage = paperId + "-" + page + ".png"
      val pngFile = configuration.buildDir(paperId) / pngPage

      if (!pngFile.exists) {

        // the generated pdf file
        val paperDir = configuration.buildDir(paperId)
        val pdfFile = paperDir / s"$paperId.pdf"

        if (pdfFile.exists) {

          // if the pdf file was generated (or at least one was generated last time...)
          for(doc <- managed(PDDocument.load(pdfFile))) {
            val imageWriter = new PDFImageWriter
            imageWriter.writeImage(doc, "png", null, page, page,
              (configuration.buildDir(paperId) / paperId).getCanonicalPath + "-",
              BufferedImage.TYPE_INT_RGB, density)
          }
        }
      }

      if (pngFile.exists) {
        Try(for(is <- managed(new FileInputStream(pngFile))) {

          val array =
            Iterator.continually(is.read).takeWhile(_ != -1). map(_.toByte).toArray

          talk.setContentType(HMime.png)
            .setContentLength(array.length)
            .write(array)
        })
      } else {
        Try(
          talk
            .setStatus(HStatus.NotFound)
            .writeJson(ErrorResponse("not_found", "page $page not found for paper $paperId")))
      }

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "Only authors and reviewers may see compiled paper")))

  }

}

