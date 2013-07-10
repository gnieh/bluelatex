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

import tiscaf._

import gnieh.sohva.sync._

import com.typesafe.config.Config

/** mixin this trait to access CouchDB facilities
 *
 *  @author Lucas Satabin
 */
trait CouchSupport {

  val config: Config

  lazy val couchConfig =
    new CouchConfiguration(config)

  /** Returns the current couch session object to issue queries to the database */
  def couchSession(implicit talk: HTalk): Option[CouchSession] =
    talk.ses.get(SessionKeys.Couch).collect {
      case sess: CouchSession => sess
    }

  /** Indicates whether the current session is a logged in user */
  def loggedIn(implicit talk: HTalk): Boolean =
    couchSession.map(_.isLoggedIn).getOrElse(false)

  /** Indicates whether the current session complies with role name */
  def hasRole(role: String)(implicit talk: HTalk): Boolean =
    couchSession match {
      case Some(session) => session.hasRole(role)
      case None => role != null && role.isEmpty
    }

  /** Returns the current user information */
  def currentUser(implicit talk: HTalk): Option[User] =
    for {
      session <- couchSession
      user <- session.currentUser
      doc <- session.database(couchConfig.database("blue_users"))
        .getDocById[User]("org.couchdb.user:" + user.name)
    } yield doc

  /** Returns the view object identified by database, design name and view name */
  def view[Key: Manifest, Value: Manifest](dbName: String, designName: String, viewName: String)(implicit talk: HTalk): Option[View[Key, Value,
  Any]] =
    for {
      couch <- couchSession
      db = couch.database(couchConfig.database(dbName))
      design = db.design(designName)
    } yield design.view[Key, Value, Any](viewName)

  /** Returns the database object identified by its name */
  def database(name: String)(implicit talk: HTalk): Option[Database] =
    for {
      couch <- couchSession
    } yield couch.database(couchConfig.database(name))

}
