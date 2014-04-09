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
package session

import com.typesafe.config.Config

import http._
import common._

import tiscaf._

import scala.util.{
  Try,
  Success
}

import gnieh.sohva.control.CouchClient

/** Log the user in.
 *  It delegates to the CouchDB login system and keeps track of the CouchDB cookie
 *
 *  @author Lucas Satabin
 */
class LoginLet(val couch: CouchClient, config: Config, logger: Logger) extends SyncBlueLet(config, logger) {

  def act(talk: HTalk): Try[Any] = {
    implicit val t = talk
    (talk.req.param("username"), talk.req.param("password")) match {
      case (Some(username), Some(password)) =>
        couchSession.login(username, password) map {
          case true  =>
            // save the current user name in the session
            talk.ses(SessionKeys.Username) = username
            talk.writeJson(true)
          case false =>
            talk
              .setStatus(HStatus.Unauthorized)
              .writeJson(ErrorResponse("unable_to_login", "Wrong username and/or password"))
        }
      case (_, _) =>
        Success(talk
          .setStatus(HStatus.BadRequest)
          .writeJson(ErrorResponse("unable_to_login", "Missing login information")))
    }
  }

}
