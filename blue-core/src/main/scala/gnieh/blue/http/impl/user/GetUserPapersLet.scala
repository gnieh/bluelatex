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
package user

import couch.{
  User,
  UserRole
}
import common._

import com.typesafe.config.Config

import tiscaf._


import scala.io.Source

import scala.util.Try

/** Returns the list of paper a user is involved in, together with his role for this paper.
 *
 *  @author Lucas Satabin
 */
class GetUserPapersLet(username: String, config: Config, logger: Logger) extends SyncBlueLet(config, logger) with SyncAuthenticatedLet {

  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Try[Unit] =
    // only authenticated users may see other people information
    view[String, UserRole, Any](blue_papers, "papers", "for").query(key = Some(username)) map { res =>
      val roles = res.values
      val result = (for((_, userRole) <- roles)
        yield userRole).toList
      talk.writeJson(result)
    }

}

