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

import couch.User
import common._

import com.typesafe.config.Config

import java.util.Calendar

import scala.util.{
  Try,
  Success,
  Failure,
  Random
}

/** Handle registration of a new user into the system.
 *  When a user is created it is created with a randomly generated password and a password reset
 *  token is created and sent to the user email address, so that he can confirm both his email
 *  address and it account. He then must set a new password.
 *
 *  @author Lucas Satabin
 */
class RegisterUserLet(config: Config, templates: Templates, mailAgent: MailAgent, recaptcha: ReCaptcha) extends BlueLet(config) {

  // TODO logging

  def act(talk: HTalk): Try[Unit] =
    if(recaptcha.verify(talk)) {
      val result = for {
        // the mandatory fields
        username <- talk.req.param("username")
        firstName <- talk.req.param("first_name")
        lastName <- talk.req.param("last_name")
        email <- talk.req.param("email_address")
        affiliation = talk.req.param("affiliation")
      } yield {
        couchConfig.asAdmin { session =>
          // generate a random password
          val password = session._uuid.getOrElse(Random.nextString(20))
          // first save the standard couchdb user
          session.users.add(username, password, couchConfig.defaultRoles) flatMap {
            case true =>
              // now the user is registered as standard couchdb user, we can add the \BlueLaTeX specific data
              val user = User(username, firstName, lastName, email, affiliation)
              val db = session.database(blue_users)
              db.saveDoc(user) flatMap {
                case Some(user) =>
                  // the user is now registered
                  // generate the password reset token
                  val cal = Calendar.getInstance
                  cal.add(Calendar.MILLISECOND, couchConfig.tokenValidity)
                  session.users.generateResetToken(username, cal.getTime) map {
                    case Some(token) =>
                      // send the confirmation email
                      val email = templates.layout("emails/register",
                        "firstName" -> firstName,
                        "baseUrl" -> config.getString("blue.base_url"),
                        "name" -> username,
                        "token" -> token,
                        "validity" -> (couchConfig.tokenValidity / 24 / 3600 / 1000))
                      mailAgent.send(username, "Welcome to \\BlueLaTeX", email)
                      (HStatus.Created, true)
                    case None =>
                      // TODO log it
                      Try(session.users.delete(username))
                      Try(db.deleteDoc(user._id))
                      (HStatus.InternalServerError,
                        ErrorResponse("unable_to_register", s"Something went wrong when registering the user $username. Please retry"))
                  }
                case None =>
                  // TODO log it
                  // somehow we couldn't save it
                  // remove the couchdb user from database
                  Try(session.users.delete(username))
                  // send error
                  Success((HStatus.InternalServerError,
                    ErrorResponse("unable_to_register", s"Something went wrong when registering the user $username. Please retry")))
              }
            case false =>
              // TODO log it
              Success((HStatus.InternalServerError,
                ErrorResponse("unable_to_register", s"Something went wrong when registering the user $username. Please retry")))
          } recover {
            case c: gnieh.sohva.ConflictException =>
              // TODO log it
              (HStatus.Conflict,
                ErrorResponse("unable_to_register", s"The user $username already exists"))
          }
        }
      }

      result.getOrElse(Success((HStatus.BadRequest, ErrorResponse("unable_to_register", "Missing parameters")))) map {
        case (status, response) => talk.setStatus(status).writeJson(response)
      }

    } else {
      // TODO log it
      Success(talk
        .setStatus(HStatus.Unauthorized)
        .writeJson(ErrorResponse("not_authorized", "ReCaptcha did not verify")))
    }

}
