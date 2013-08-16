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

import tiscaf._

import couch._

import com.typesafe.config.Config

import net.liftweb.json._

/** Enriches the standard tiscaf `HTalk` object with methods that are useful in \BlueLaTeX
 *
 *  @author Lucas Satabin
 */
class RichTalk(val talk: HTalk) extends AnyVal {

  /** Serializes the value to its json representation and writes the response to the client,
   *  corrrectly setting the result type and length */
  def writeJson(json: Any): HTalk = {
    val response = pretty(render(Extraction.decompose(json)(DefaultFormats)))
    talk
      .setContentType(HMime.json)
      .setContentLength(response.length)
      .write(response)
  }

}

/** All modules in \BlueLaTeX should implement `BlueLet` or one of its derivatives
 *
 *  @author Lucas Satabin
 */
abstract class BlueLet(val config: Config) extends HSimpleLet with CouchSupport {

  import scala.language.implicitConversions

  @inline
  implicit def talk2rich(talk: HTalk): RichTalk =
    new RichTalk(talk)

}

/** Extend this class if you need to treat differently authenticated and unauthenticated
 *  users.
 *
 *  @author Lucas Satabin
 */
abstract class AuthenticatedLet(config: Config) extends BlueLet(config) {

  final def act(talk: HTalk): Unit =
    currentUser(talk) match {
      case Some(user) =>
        loggedInAct(user)(talk)
      case None =>
        loggedOutAct(talk)
    }

  /** The action to take when the user is authenticated */
  def loggedInAct(user: User)(implicit talk: HTalk): Unit

  /** The action to take when the user is not authenticated.
   *  By default sends an error object with code "Unauthorized"
   */
  def loggedOutAct(implicit talk: HTalk): Unit = {
    talk
      .setStatus(HStatus.Unauthorized)
      .writeJson(ErrorResponse("unauthorized", "This action is only permitted to authenticated people"))
  }

}

/** Extends this class if you need to treat differently authors, reviewers or other users
 *  for a given paper.
 *
 *  @author Lucas Satabin
 */
abstract class RoleLet(val paperId: String, config: Config) extends AuthenticatedLet(config) {

  private def roles(implicit talk: HTalk): Map[String, PaperRole] =
    (for {
      session <- couchSession
      Paper(_, _, authors, reviewers, _, _, _, _, _, _) <- session.database(
        couchConfig.database("blue_papers")).getDocById[Paper](paperId)
    } yield {
      (authors.map(id => (id, Author)) ++
        reviewers.map(id => (id, Reviewer))).toMap.withDefaultValue(Other)
    }).getOrElse(Map().withDefaultValue(Other))

  final def loggedInAct(user: User)(implicit talk: HTalk): Unit =
    roleAct(user, roles(talk)(user._id))

  /** Implement this method that can behave differently depending on the user
   *  role for the current paper.
   *  It is only called when the user is authenticated
   */
  def roleAct(user: User, role: PaperRole)(implicit talk: HTalk): Unit

}
