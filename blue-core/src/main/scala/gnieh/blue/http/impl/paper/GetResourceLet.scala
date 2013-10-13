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

import java.io.FileInputStream

import tiscaf._

import gnieh.sohva.UserInfo

import resource._

import scala.util.Try

/** Retrieves some resource associated to the paper.
 *
 *  @author Lucas Satabin
 */
class GetResourceLet(resourceName: String, paperId: String, config: Config) extends RoleLet(paperId, config) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Unit] = Try(role match {
    case Author =>
      // only authors may get a resource
      val file = configuration.resource(paperId, resourceName)

      if (file.exists) {
        // returns the resource file
        for(fis <- managed(new FileInputStream(file))) {
          val length = fis.available
          val array = new Array[Byte](length)
          fis.read(array)

          import FileProcessing._

          val mime = HMime.exts.get(file.extension.toLowerCase).getOrElse("application/octet-stream")

          talk.setContentLength(length)
            .setContentType(mime)
            .write(array)
        }
      } else {
        // resource not found => error 404
        talk
          .writeJson(ErrorResponse("unknown_resource",s"Unable to find resource $resourceName for paper $paperId"))
          .setStatus(HStatus.NotFound)
      }
    case _ =>
      talk
        .writeJson(ErrorResponse("no_sufficient_rights", "Only authors may upload resources"))
        .setStatus(HStatus.Forbidden)
  })

}

