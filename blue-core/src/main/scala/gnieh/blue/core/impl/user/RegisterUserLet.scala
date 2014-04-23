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
package impl.user

import tiscaf._

import http._
import couch.User
import common._

import com.typesafe.config.Config

import org.osgi.framework.BundleContext

import java.util.Calendar

import scala.util.{
  Try,
  Success,
  Failure,
  Random
}

import gnieh.sohva.control.CouchClient

/** Handle registration of a new user into the system.
 *  When a user is created it is created with a randomly generated password and a password reset
 *  token is created and sent to the user email address, so that he can confirm both his email
 *  address and it account. He then must set a new password.
 *
 *  @author Lucas Satabin
 */
class RegisterUserLet(val couch: CouchClient, config: Config, context: BundleContext, templates: Templates, mailAgent: MailAgent, recaptcha: ReCaptcha, logger: Logger)
    extends SyncBlueLet(config, logger) {

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
        couchConfig.asAdmin(couch) { session =>
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
                  cal.add(Calendar.SECOND, couchConfig.tokenValidity)
                  session.users.generateResetToken(username, cal.getTime) map {
                    case Some(token) =>
                      // send the confirmation email
                      val email = templates.layout("emails/register",
                        "firstName" -> firstName,
                        "baseUrl" -> config.getString("blue.base_url"),
                        "name" -> username,
                        "token" -> token,
                        "validity" -> (couchConfig.tokenValidity / 24 / 3600))
                      mailAgent.send(username, "Welcome to \\BlueLaTeX", email)

                      import OsgiUtils._
                      // notifiy creation hooks
                      couchConfig.asAdmin(couch) { session =>
                        for(hook <- context.getAll[UserRegistered])
                          Try(hook.afterRegister(username, session))
                      }

                      (HStatus.Created, true)
                    case None =>
                      logWarn(s"Unable to generate password reset token for user $username")
                      Try(session.users.delete(username))
                      Try(db.deleteDoc(user._id))
                      (HStatus.InternalServerError,
                        ErrorResponse("unable_to_register", s"Something went wrong when registering the user $username. Please retry"))
                  }
                case None =>
                  logWarn(s"Unable to create \\BlueLaTeX user $username")
                  // somehow we couldn't save it
                  // remove the couchdb user from database
                  Try(session.users.delete(username))
                  // send error
                  Success((HStatus.InternalServerError,
                    ErrorResponse("unable_to_register", s"Something went wrong when registering the user $username. Please retry")))
              }
            case false =>
              logWarn(s"Unable to create CouchDB user $username")
              Success((HStatus.InternalServerError,
                ErrorResponse("unable_to_register", s"Something went wrong when registering the user $username. Please retry")))
          } recover {
            case _: gnieh.sohva.ConflictException =>
              logWarn(s"User $username already exists")
              (HStatus.Conflict,
                ErrorResponse("unable_to_register", s"The user $username already exists"))
          }
        }
      }

      result.getOrElse(Success((HStatus.BadRequest, ErrorResponse("unable_to_register", "Missing parameters")))) map {
        case (status, response) => talk.setStatus(status).writeJson(response)
      }

    } else {
      logWarn(s"ReCaptcha did not verify when trying to create a user")
      Success(talk
        .setStatus(HStatus.Unauthorized)
        .writeJson(ErrorResponse("not_authorized", "ReCaptcha did not verify")))
    }

}
