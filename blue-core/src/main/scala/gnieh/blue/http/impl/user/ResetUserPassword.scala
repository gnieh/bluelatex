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
package impl.user

import tiscaf._

import common._

import com.typesafe.config.Config

import scala.util.{
  Try,
  Success,
  Failure
}

/** Performs the password reset action for a given user.
 *
 *  @author Lucas Satabin
 */
class ResetUserPassword(username: String, config: Config, logger: Logger) extends BlueLet(config, logger) {

  def act(talk: HTalk): Try[Unit] = {
    val token = talk.req.param("reset_token")
    val password1 = talk.req.param("new_password1")
    val password2 = talk.req.param("new_password2")
    (token, password1, password2) match {
      case (Some(token), Some(password1), Some(password2)) if password1 == password2 =>
        // all parameters given, and passwords match, proceed
        couchConfig.asAdmin { sess =>
          sess.users.resetPassword(username, token, password1) map {
            case true =>
              talk.writeJson(true)
            case false =>
              talk.writeJson(ErrorResponse("unable_to_reset", "Cannot perform password reset"))
                .setStatus(HStatus.InternalServerError)
          }
        }
      case (t, p1, p2) =>
        // a parameter is missing or password do not match
        Success(talk.writeJson(ErrorResponse("unable_to_reset", "Wrong parameters"))
          .setStatus(HStatus.BadRequest))
    }
  }

}

