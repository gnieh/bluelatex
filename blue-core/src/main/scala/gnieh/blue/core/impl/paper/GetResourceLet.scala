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

import java.io.FileInputStream

import tiscaf._

import resource._

import http._
import common._
import permission._

import scala.util.Try

import gnieh.sohva.control.CouchClient

/** Retrieves some resource associated to the paper.
 *
 *  @author Lucas Satabin
 */
class GetResourceLet(paperId: String, resourceName: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: Option[UserInfo], role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = Try(permissions match {
    case Edit() =>
      // only authors may get a resource
      val file = configuration.resource(paperId, resourceName)

      if (file.exists) {
        // returns the resource file
        for(fis <- managed(new FileInputStream(file))) {
          val length = fis.available
          val array = new Array[Byte](length)
          fis.read(array)

          import FileUtils._

          val mime = HMime.exts.get(file.extension.tail.toLowerCase).getOrElse("application/octet-stream")

          talk.setContentLength(length)
            .setContentType(mime)
            .write(array)
        }
      } else {
        // resource not found => error 404
        talk
          .setStatus(HStatus.NotFound)
          .writeJson(ErrorResponse("unknown_resource",s"Unable to find resource $resourceName for paper $paperId"))
      }
    case _ =>
      talk
        .setStatus(HStatus.Forbidden)
        .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to see resources"))
  })

}

