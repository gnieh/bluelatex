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

import user._
import session._
import paper._

import com.typesafe.config.Config

/** The core Api providing features to:
 *   - manage users
 *   - manage sessions
 *   - manage papers
 *   - synchronize papers
 *
 *  @author Lucas Satabin
 */
class CoreApi(config: Config, syncServer: SynchroServer, templates: Templates, mailAgent: MailAgent, recaptcha: ReCaptcha) extends RestApi {

  POST {
    // registers a new user
    case p"users" =>
      new RegisterUserLet(config, templates, mailAgent, recaptcha)
    // performs password reset
    case p"users/$username/reset" =>
      new ResetUserPassword(username, config)
    // creates a new paper
    case p"papers" =>
      new CreatePaperLet(config, templates)
    // log a user in
    case p"session" =>
      new LoginLet(config)
    // synchronization request
    case p"papers/$paperid/sync" =>
      new SynchronizePaperLet(paperid, config, syncServer)
  }

  PATCH {
    // save the data for the authenticated user
    case p"users/$username/info" =>
      new ModifyUserLet(username, config)
    // add or remove people involved in this paper (authors, reviewers), change modules, tag, branch, ...
    case p"papers/$paperid/info" =>
      new ModifyPaperLet(paperid, config)
  }

  GET {
    // gets the data of the given user
    case p"users/$username/info" =>
      new GetUserInfoLet(username, config)
    // gets the list of papers the given user is involved in
    case p"users/$username/papers" =>
      new GetUserPapersLet(username, config)
    // generates a password reset token
    case p"users/$username/reset" =>
      new GeneratePasswordReset(username, templates, mailAgent, config)
    // gets the currently logged in user information
    case p"session" =>
      new GetSessionDataLet(config)
    // gets the list of people involved in this paper with their role, the currently
    // enabled modules, the tags, the branch, ...
    case p"papers/$paperid/info" =>
      new GetPaperInfoLet(paperid, config)
    // downloads a zip archive containing the paper files
    case p"papers/$paperid.zip" =>
      new BackupPaperLet("zip", paperid, config)
    // downloads the list of synchronized resources
    case p"papers/$paperid/files/synchronized" =>
      new SynchronizedResourcesLet(paperid, config)
    // downloads the list of non synchronized resources
    case p"papers/$paperid/files/resources" =>
      new NonSynchronizedResourcesLet(paperid, config)
  }

  DELETE {
    // unregisters the authenticated user
    case p"users/$username" =>
      ???
    // log a user out
    case p"session" =>
      new LogoutLet(config)
    // deletes a paper
    case p"papers/$paperid" =>
      new DeletePaperLet(paperid, config, recaptcha)
  }

}
