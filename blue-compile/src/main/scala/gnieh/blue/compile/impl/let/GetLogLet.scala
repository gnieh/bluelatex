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

import com.typesafe.config.Config

import scala.util.Try

import scala.io.Source


import resource._

class GetLogLet(paperId: String, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Any] = role match {
    case Author =>

      import FileProcessing._

      val logFile = configuration.buildDir(paperId) / s"$paperId.log"

      if(logFile.exists)
        Try(for(log <- managed(Source.fromFile(logFile))) {

          val text = log.mkString

          talk.setContentType(HMime.txt)
            .setContentLength(text.length)
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
          .writeJson(ErrorResponse("no_sufficient_rights", "Only authors may see compilation results")))

  }

}

