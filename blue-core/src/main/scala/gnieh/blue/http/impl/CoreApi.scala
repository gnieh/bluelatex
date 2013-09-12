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
class CoreApi(config: Config, templates: Templates, mailAgent: MailAgent, recaptcha: ReCaptcha) extends RestApi {

  POST {
    // registers a new user
    case p"users" =>
      new RegisterUserLet(config, templates, mailAgent, recaptcha)
    // performs password reset
    case p"users/$username/reset" =>
      ???
    // creates a new paper
    case p"papers" =>
      new CreatePaperLet(config, templates)
    // save the authors involved in this paper (authors, reviewers)
    case p"papers/$paperid/people" =>
      ???
    // log a user in
    case p"session" =>
      new LoginLet(config)
    // synchronization request
    case p"sync" =>
      ???
  }

  PATCH {
    // save the data for the authenticated user
    case p"users/$username/info" =>
      ???
  }

  GET {
    // gets the data of the given user
    case p"users/$username/info" =>
      ???
    // gets the list of papers the given user is involved in
    case p"users/$username/papers" =>
      ???
    // generates a password reset token
    case p"users/$username/reset" =>
      ???
    // gets the currently logged in user information
    case p"session" =>
      ???
    // gets the list of people involved in this paper with their role
    case p"papers/$paperid/people" =>
      ???
    // downloads a zip archive containing the paper files
    case p"papers/$paperid.zip" =>
      ???
    // downloads the list of synchronized resources
    case p"papers/$paperid/files/synchronized" =>
      ???
    // downloads the list of non synchronized resources
    case p"papers/$paperid/files/resources" =>
      ???
  }

  DELETE {
    // unregisters the authenticated user
    case p"users/$username" =>
      ???
    // log a user out
    case p"session" =>
      ???
    // deletes a paper
    case p"papers/$paperid" =>
      ???
  }

}
