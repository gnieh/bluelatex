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

import couch.User

import common._

import tiscaf._

import gnieh.sohva.control._

import com.typesafe.config.Config

import scala.util.Try

/** mixin this trait to access CouchDB facilities
 *
 *  @author Lucas Satabin
 */
trait CouchSupport {

  val config: Config

  val couch: CouchClient

  lazy val couchConfig =
    new CouchConfiguration(config)

  /** Returns the current couch session object to issue queries to the database */
  def couchSession(implicit talk: HTalk): CookieSession =
    talk.ses.get(SessionKeys.Couch).collect {
      case sess: CookieSession => sess
    } match {
      case Some(sess) => sess
      case None =>
        // start and register a new couch session
        val sess = couch.startCookieSession
        talk.ses(SessionKeys.Couch) = sess
        sess
    }

  /** Indicates whether the current session is a logged in user */
  def loggedIn(implicit talk: HTalk): Try[Boolean] =
    couchSession.isAuthenticated

  /** Indicates whether the current session complies with role name */
  def hasRole(role: String)(implicit talk: HTalk): Try[Boolean] =
    couchSession .hasRole(role)

  /** Returns the current user information */
  def currentUser(implicit talk: HTalk): Try[Option[UserInfo]] =
    couchSession.currentUser

  /** Returns the view object identified by database, design name and view name */
  def view(dbName: String, designName: String, viewName: String)(implicit talk: HTalk): View = {
      val db = couchSession.database(couchConfig.database(dbName))
      val design = db.design(designName)
      design.view(viewName)
  }

  /** Returns the database object identified by its name */
  def database(name: String)(implicit talk: HTalk): Database =
    couchSession.database(couchConfig.database(name))

  def dbname(name: String): String =
    couchConfig.database(name)

  val blue_users =
    dbname("blue_users")

  val blue_papers =
    dbname("blue_papers")

}
