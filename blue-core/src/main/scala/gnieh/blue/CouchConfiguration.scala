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

import com.typesafe.config.Config

import gnieh.sohva.sync._

import java.io.File

import scala.collection.JavaConverters._

/** The couchdb configuration part */
class CouchConfiguration(config: Config) {

  val couch = {
    val hostname = config.getString("couch.hostname")
    val port = config.getInt("couch.port")
    val ssl = config.getBoolean("couch.ssl")
    new CouchClient(host = hostname, port = port, ssl = ssl)
  }

  val couchAdminName = config.getString("couch.admin-name")

  val couchAdminPassword = config.getString("couch.admin-password")

  val couchDesigns = new File(config.getString("couch.design.dir"))

  def adminSession = {
    val sess = couch.startSession
    sess.login(couchAdminName, couchAdminPassword)
    sess
  }

  def asAdmin[T](code: CouchSession => T) = {
    val sess = adminSession
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
    config.getList("couch.user.roles").asScala.map(_.render).toList

  val tokenValidity =
    config.getMilliseconds("couch.user.token_validity").toInt

}

