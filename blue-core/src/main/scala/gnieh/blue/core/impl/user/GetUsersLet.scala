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
package user

import http._
import couch.User
import common._

import com.typesafe.config.Config

import tiscaf._

import gnieh.sohva.UserInfo

import scala.io.Source

import scala.util.Try

/** Return a (potentially filtered) list of user names.
 *
 *  @author Lucas Satabin
 */
class GetUsersLet(config: Config, logger: Logger) extends SyncBlueLet(config, logger) with SyncAuthenticatedLet {

  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Try[Any] = {
    val userNames = view[String, String, Any](blue_users, "lists", "names")
    // get the filter parameter if any
    val filter = talk.req.param("name")
    val startkey = filter.map(_.toLowerCase)
    val endkey = startkey.map(_.toLowerCase + "Z")
    for(users <- userNames.query(startkey = startkey, endkey = endkey))
      yield talk.writeJson(users.rows.map(_.key))
  }

}

