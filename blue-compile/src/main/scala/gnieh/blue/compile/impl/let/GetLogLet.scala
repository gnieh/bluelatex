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

import scala.io.{
  Source,
  Codec
}

import resource._

import gnieh.sohva.control.CouchClient

import java.nio.charset.CodingErrorAction

object GetLogLet {

  val codec = Codec.UTF8.onMalformedInput(CodingErrorAction.REPLACE)

}

class GetLogLet(paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: Option[UserInfo], role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Any] = permissions match {
    case Compile() =>

      import FileUtils._

      val logFile = configuration.buildDir(paperId) / s"main.log"

      if(logFile.exists)
        Try(for(log <- managed(Source.fromFile(logFile)(GetLogLet.codec))) {

          val text = log.mkString.getBytes("UTF-8")

          talk.setContentType("${HMime.txt};charset=${talk.encoding}")
            .setContentLength(text.size)
            .setFilename(logFile.getName)
            .write(text)
        })
      else
        Try(
          talk
            .setStatus(HStatus.NotFound)
            .writeJson(ErrorResponse("not_found", "compilation log for paper $paperId not found")))

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to see compilation results")))

  }

}

