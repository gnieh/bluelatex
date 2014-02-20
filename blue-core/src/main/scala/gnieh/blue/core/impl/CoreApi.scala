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

/** The core Api providing features to:
 *   - manage users
 *   - manage sessions
 *   - manage papers
 *
 *  @author Lucas Satabin
 */
class CoreApi(config: Config, system: ActorSystem, context: BundleContext, templates: Templates, mailAgent: MailAgent, recaptcha: ReCaptcha, logger: Logger) extends RestApi {

  POST {
    // registers a new user
    case p"users" =>
      new RegisterUserLet(config, context, templates, mailAgent, recaptcha, logger)
    // performs password reset
    case p"users/$username/reset" =>
      new ResetUserPassword(username, config, logger)
    // creates a new paper
    case p"papers" =>
      new CreatePaperLet(config, context, templates, logger)
    // join a paper
    case p"papers/$paperid/join" =>
      new JoinPaperLet(paperid, system, config, logger)
    // leave a paper
    case p"papers/$paperid/part" =>
      new PartPaperLet(paperid, system, config, logger)
    // log a user in
    case p"session" =>
      new LoginLet(config, logger)
    // save a non synchronized resource
    case p"papers/$paperid/files/resources/$resourcename" =>
      new SaveResourceLet(paperid, resourcename, config, logger)
  }

  PATCH {
    // save the data for the authenticated user
    case p"users/$username/info" =>
      new ModifyUserLet(username, config, logger)
    // add or remove people involved in this paper (authors, reviewers), change modules, tag, branch, ...
    case p"papers/$paperid/info" =>
      new ModifyPaperLet(paperid, config, logger)
  }

  private val GetUsersLet = new GetUsersLet(config, logger)

  GET {
    // gets the list of users matching the given pattern
    case p"users" =>
      GetUsersLet
    // gets the data of the given user
    case p"users/$username/info" =>
      new GetUserInfoLet(username, config, logger)
    // gets the list of papers the given user is involved in
    case p"users/$username/papers" =>
      new GetUserPapersLet(username, config, logger)
    // generates a password reset token
    case p"users/$username/reset" =>
      new GeneratePasswordReset(username, templates, mailAgent, config, logger)
    // gets the currently logged in user information
    case p"session" =>
      new GetSessionDataLet(config, logger)
    // gets the list of people involved in this paper with their role, the currently
    // enabled modules, the tags, the branch, ...
    case p"papers/$paperid/info" =>
      new GetPaperInfoLet(paperid, config, logger)
    // downloads a zip archive containing the paper files
    case p"papers/$paperid/zip" =>
      new BackupPaperLet("zip", paperid, config, logger)
    // downloads the list of synchronized resources
    case p"papers/$paperid/files/synchronized" =>
      new SynchronizedResourcesLet(paperid, config, logger)
    // downloads the list of non synchronized resources
    case p"papers/$paperid/files/resources" =>
      new NonSynchronizedResourcesLet(paperid, config, logger)
    // gets a non synchronized resource
    case p"papers/$paperid/files/resources/$resourcename" =>
      new GetResourceLet(paperid, resourcename, config, logger)
  }

  DELETE {
    // unregisters the authenticated user
    case p"users/$username" =>
      new DeleteUserLet(username, context, config, recaptcha, logger)
    // log a user out
    case p"session" =>
      new LogoutLet(config, logger)
    // deletes a paper
    case p"papers/$paperid" =>
      new DeletePaperLet(paperid, context, config, recaptcha, logger)
    // deletes a non synchronized resource
    case p"papers/$paperid/files/resources/$resourcename" =>
      new DeleteResourceLet(paperid, resourcename, config, logger)
  }

}
