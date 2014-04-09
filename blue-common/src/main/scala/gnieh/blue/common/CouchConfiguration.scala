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
package common

import com.typesafe.config.Config

import gnieh.sohva.control._

import java.io.File

import scala.collection.JavaConverters._

import java.util.concurrent.TimeUnit

/** The couchdb configuration part */
class CouchConfiguration(val config: Config) {

  val couchAdminName = config.getString("couch.admin-name")

  val couchAdminPassword = config.getString("couch.admin-password")

  val couchDesigns = new File(config.getString("couch.design.dir"))

  def adminSession(client: CouchClient) = {
    val sess = client.startCookieSession
    sess.login(couchAdminName, couchAdminPassword)
    sess
  }

  def asAdmin[T](client: CouchClient)(code: CookieSession => T) = {
    val sess = adminSession(client)
    try {
      code(sess)
    } finally {
      sess.logout
    }
  }

  def designDir(dbName: String) =
    new File(couchDesigns, dbName)

  def database(key: String) = {
    val k = s"couch.database.$key"
    if(config.hasPath(k))
      config.getString(k)
    else
      key
  }

  /** The list of \BlueLaTeX databases */
  val databases =
    config.getObject("couch.database").asScala.keys.toList

  /** The list of default roles to assign to a newly created user */
  val defaultRoles =
    config.getList("couch.user.roles").unwrapped.asScala.map(_.toString).toList

  /** The reset password token validity in seconds */
  val tokenValidity =
    config.getDuration("couch.user.token-validity", TimeUnit.SECONDS).toInt

}

