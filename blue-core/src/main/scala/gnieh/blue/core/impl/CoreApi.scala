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

import http._
import common._
import user._
import session._
import paper._

import com.typesafe.config.Config

import akka.actor.ActorSystem

import org.osgi.framework.BundleContext

import gnieh.sohva.control.CouchClient

/** The core Api providing features to:
 *   - manage users
 *   - manage sessions
 *   - manage papers
 *
 *  @author Lucas Satabin
 */
class CoreApi(
  couch: CouchClient,
  val config: Config,
  system: ActorSystem,
  context: BundleContext,
  templates: Templates,
  mailAgent: MailAgent,
  recaptcha: ReCaptcha,
  logger: Logger) extends RestApi {

  POST {
    // registers a new user
    case p"users" =>
      new RegisterUserLet(couch, config, context, templates, mailAgent, recaptcha, logger)
    // performs password reset
    case p"users/$username/reset" =>
      new ResetUserPassword(username, couch, config, logger)
    // creates a new paper
    case p"papers" =>
      new CreatePaperLet(couch, config, context, templates, logger)
    // join a paper
    case p"papers/$paperid/join/$peerid" =>
      new JoinPaperLet(paperid, peerid, system, couch, config, logger)
    // leave a paper
    case p"papers/$paperid/part/$peerid" =>
      new PartPaperLet(paperid, peerid, system, couch, config, logger)
    // log a user in
    case p"session" =>
      new LoginLet(couch, config, logger)
    // save a non synchronized resource
    case p"papers/$paperid/files/resources/$resourcename" =>
      new SaveResourceLet(paperid, resourcename, couch, config, logger)
  }

  PATCH {
    // save the data for the authenticated user
    case p"users/$username/info" =>
      new ModifyUserLet(username, couch, config, logger)
    // modify paper information such as paper name
    case p"papers/$paperid/info" =>
      new ModifyPaperLet(paperid, couch, config, logger)
    // add or remove people involved in this paper (authors, reviewers)
    case p"papers/$paperid/roles" =>
      new ModifyRolesLet(paperid, couch, config, logger)
    // add or remove permissions for each role in this paper
    case p"papers/$paperid/permissions" =>
      new ModifyPermissionsLet(paperid, couch, config, logger)
  }

  private val GetUsersLet = new GetUsersLet(couch, config, logger)

  GET {
    // gets the list of users matching the given pattern
    case p"users" =>
      GetUsersLet
    // gets the data of the given user
    case p"users/$username/info" =>
      new GetUserInfoLet(username, couch, config, logger)
    // gets the list of papers the given user is involved in
    case p"users/$username/papers" =>
      new GetUserPapersLet(username, couch, config, logger)
    // generates a password reset token
    case p"users/$username/reset" =>
      new GeneratePasswordReset(username, templates, mailAgent, couch, config, logger)
    // gets the currently logged in user information
    case p"session" =>
      new GetSessionDataLet(couch, config, logger)
    // gets the list of people involved in this paper with their role
    case p"papers/$paperid/roles" =>
      new GetPaperRolesLet(paperid, couch, config, logger)
    // gets the list of permissions for each role in this paper
    case p"papers/$paperid/permissions" =>
      new GetPaperPermissionsLet(paperid, couch, config, logger)
    // gets the paper data
    case p"papers/$paperid/info" =>
      new GetPaperInfoLet(paperid, couch, config, logger)
    // downloads a zip archive containing the paper files
    case p"papers/$paperid/zip" =>
      new BackupPaperLet("zip", paperid, couch, config, logger)
    // downloads the list of synchronized resources
    case p"papers/$paperid/files/synchronized" =>
      new SynchronizedResourcesLet(paperid, couch, config, logger)
    // downloads the list of non synchronized resources
    case p"papers/$paperid/files/resources" =>
      new NonSynchronizedResourcesLet(paperid, couch, config, logger)
    // gets a non synchronized resource
    case p"papers/$paperid/files/resources/$resourcename" =>
      new GetResourceLet(paperid, resourcename, couch, config, logger)
  }

  DELETE {
    // unregisters the authenticated user
    case p"users/$username" =>
      new DeleteUserLet(username, context, couch, config, recaptcha, logger)
    // log a user out
    case p"session" =>
      new LogoutLet(couch, config, logger)
    // deletes a paper
    case p"papers/$paperid" =>
      new DeletePaperLet(paperid, context, couch, config, recaptcha, logger)
    // deletes a non synchronized resource
    case p"papers/$paperid/files/resources/$resourcename" =>
      new DeleteResourceLet(paperid, resourcename, couch, config, logger)
  }

}
