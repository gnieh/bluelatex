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

import scala.util.{
  Try,
  Success,
  Failure
}

import gnieh.sohva.UserInfo

/** Handle request that want to access the compiler data
 *
 *  @author Lucas Satabin
 */
class GetCompilerSettingsLet(paperId: String, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Any] = role match {
    case Author =>
      // only authors users may see other compiler settings
      database(blue_papers).getDocById[CompilerSettings](s"$paperId:compiler") map {
        // we are sure that the settings has a revision because it comes from the database
        case Some(settings) =>
          talk.writeJson(settings, settings._rev.get)
        case None =>
          talk.writeJson(ErrorResponse("not_found", s"No compiler for paper $paperId found")).setStatus(HStatus.NotFound)
      }

    case _ =>
      Try(talk.writeJson(ErrorResponse("no_sufficient_rights", "Only authors may see compiler settings"))
        .setStatus(HStatus.Forbidden))

  }

}

