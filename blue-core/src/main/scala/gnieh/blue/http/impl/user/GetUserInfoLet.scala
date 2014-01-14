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

import couch.User
import common._

import com.typesafe.config.Config

import tiscaf._


import scala.io.Source

import scala.util.Try

/** Returns the user data
 *
 *  @author Lucas Satabin
 */
class GetUserInfoLet(username: String, config: Config, logger: Logger) extends SyncBlueLet(config, logger) with SyncAuthenticatedLet {

  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Try[Unit] =
    // only authenticated users may see other people information
    database(blue_users).getDocById[User](s"org.couchdb.user:$username") map {
      // we are sure that the user has a revision because it comes from the database
      case Some(user) => talk.writeJson(user, user._rev.get)
      case None       => talk.setStatus(HStatus.NotFound).writeJson(ErrorResponse("not_found", s"No user named $username found"))
    }

}

