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
package session

import com.typesafe.config.Config

import tiscaf._

import scala.util.{
  Try,
  Success
}

/** Log the user in.
 *  It delegates to the CouchDB login system and keeps track of the CouchDB cookie
 *
 *  @author Lucas Satabin
 */
class LoginLet(config: Config) extends BlueLet(config) {

  def act(talk: HTalk): Try[Unit] = {
    implicit val t = talk
    (talk.req.param("username"), talk.req.param("password")) match {
      case (Some(username), Some(password)) =>
        couchSession.login(username, password) map {
          case true  => talk.writeJson(true)
          case false =>
            talk.writeJson(ErrorResponse("unable_to_login", "Wrong username and/or password"))
              .setStatus(HStatus.Unauthorized)
        }
      case (_, _) =>
        Success(talk.writeJson(ErrorResponse("unable_to_login", "Missing login information"))
          .setStatus(HStatus.BadRequest))
    }
  }

}
