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

import gnieh.sohva.control.{
  CouchClient,
  Session
}
import gnieh.sohva.{
  SohvaException,
  ConflictException
}

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
        implicit val t = talk
        couchConfig.asAdmin(couch) { session =>
          // generate a random password
          val password = session._uuid.getOrElse(Random.nextString(20))
          // first save the standard couchdb user
          session.users.add(username, password, couchConfig.defaultRoles) flatMap {
            case true =>
              val manager = entityManager("blue_users")
              // now the user is registered as standard couchdb user, we can add the \BlueLaTeX specific data
              val userid = s"org.couchdb.user:$username"
              val user = User(username, firstName, lastName, email, affiliation)
              (for {
                () <- manager.create(userid, Some("blue-user"))
                user <- manager.saveComponent(userid, user)
                _ <- sendEmail(user, session)
              } yield {
                import OsgiUtils._
                // notifiy creation hooks
                couchConfig.asAdmin(couch) { session =>
                  for(hook <- context.getAll[UserRegistered])
                    Try(hook.afterRegister(user.name, manager))
                }

                (HStatus.Created, true)
              }) recoverWith {
                case e =>
                  //logWarn(s"Unable to create \\BlueLaTeX user $username")
                  logError(s"Unable to create \\BlueLaTeX user $username", e)
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
          }
        } recover {
          case ConflictException(_) =>
            logWarn(s"User $username already exists")
            (HStatus.Conflict,
              ErrorResponse("unable_to_register", s"The user $username already exists"))
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

    private def sendEmail(user: User, session: Session) = {
      // the user is now registered
      // generate the password reset token
      val cal = Calendar.getInstance
      cal.add(Calendar.SECOND, couchConfig.tokenValidity)
      session.users.generateResetToken(user.name, cal.getTime) map { token =>
        logDebug(s"Sending confirmation email to ${user.email}")
        // send the confirmation email
        val email = templates.layout("emails/register",
          "firstName" -> user.first_name,
          "baseUrl" -> config.getString("blue.base-url"),
          "name" -> user.name,
          "token" -> token,
          "validity" -> (couchConfig.tokenValidity / 24 / 3600))
        logDebug(s"Registration email: $email")
        mailAgent.send(user.name, "Welcome to \\BlueLaTeX", email)
      } recover {
        case e =>
          logError(s"Unable to generate confirmation token for user ${user.name}", e)
      }
    }

}
