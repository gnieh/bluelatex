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

import java.io.{
  FileOutputStream,
  ByteArrayOutputStream
}

import tiscaf._

import resource._

import http._
import common._
import permission._

import scala.util.Try

import gnieh.sohva.control.CouchClient

/** Saves some resource associated to the paper.
 *
 *  @author Lucas Satabin
 */
class SaveResourceLet(paperId: String, resourceName: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  override def partsAcceptor(reqInfo: HReqHeaderData) =
    Some(new ResourcePartsAcceptor(reqInfo))

  class ResourcePartsAcceptor(reqInfo: HReqHeaderData) extends HPartsAcceptor(reqInfo) {

    private var input: ByteArrayOutputStream = _

    def open(desc: HPartDescriptor) = {
      input = new ByteArrayOutputStream
      true
    }

    def accept(bytes: Array[Byte]) = {
      input.write(bytes)
      true
    }
    def close {
      image = Some(input.toByteArray)
      input = null
    }
    def declineAll { input = null }
  }

  private var image: Option[Array[Byte]] = None

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = Try(permissions match {
    case Edit() =>
      // only authors may upload a resource
      val data = image.orElse(talk.req.octets)

      val file = configuration.resource(paperId, resourceName)

      data match {
        case Some(resourceFile) =>

          // if file does not exist, create it
          if (!file.exists)
            file.createNewFile

          // save the resource to disk
          // save it in the resource directory
          for(fos <- managed(new FileOutputStream(file))) {
            fos.write(resourceFile)
          }

          talk.writeJson(true)

        case None =>
          talk
            .setStatus(HStatus.NoContent)
            .writeJson(ErrorResponse("no_content", "No file sent to save"))

      }

    case _ =>
      talk
        .setStatus(HStatus.Forbidden)
        .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to upload resources"))
  })

}

