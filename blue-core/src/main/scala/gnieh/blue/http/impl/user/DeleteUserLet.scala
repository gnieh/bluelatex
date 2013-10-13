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
package user

import tiscaf._

import gnieh.sohva.UserInfo

import couch._

import com.typesafe.config.Config

import scala.util.{
  Try,
  Success
}

/** handles unregistration of a user.
 *  When a user unregisters, if there are papers for which he is the single author,
 *  the user cannot be unregistered and the process is aborted.
 *
 *  @author Lucas Satabin
 */
class DeleteUserLet(username: String, config: Config, recaptcha: ReCaptcha) extends AuthenticatedLet(config) {

  // TODO logging

  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Try[Unit] =
    if(user.name == username && recaptcha.verify(talk)) {

      // get the papers in which this user is involved
      view[String, UserRole](blue_papers, "papers", "for").query(key = Some(username)) flatMap { res =>
        val roles = res.values
        // get all papers for which the user is an author
        val papers = for((_, UserRole(paperId, _, "author")) <- roles)
          yield paperId

        // get the paper authors for each of these papers
        view[String, List[String]](blue_papers, "papers", "authors").query(keys = papers) map { res =>
          // the list of papers for which this user is the single author
          val singleAuthor =
            for((paperId, List(name)) <- res.values if name == username)
              yield paperId

          if(singleAuthor.isEmpty) {
            // ok no paper for which this user is the single author, let's remove him
            // first delete the \BlueLaTeX specific user document
            database(blue_users).deleteDoc(s"org.couchdb.user:$username") flatMap {
              case true =>
                // delete the couchdb user
                couchSession.users.delete(username)
              case false =>
                // TODO log it
                Success(talk
                  .writeJson(ErrorResponse("cannot_unregister", "Unable to delete user data from database"))
                  .setStatus(HStatus.InternalServerError))
            }

          } else {
            // Nope! You must first transfer the papers ownership, or delete them!
            talk
              .writeJson(ErrorResponse("cannot_unregister", s"""Your are the single author of the following papers: ${singleAuthor.mkString("[", ", ", "]")}"""))
              .setStatus(HStatus.Forbidden)
          }
        }

      }

    } else {
      // TODO log it
      Success(talk
        .setStatus(HStatus.Unauthorized)
        .writeJson(ErrorResponse("not_authorized", "ReCaptcha did not verify")))
    }

}

