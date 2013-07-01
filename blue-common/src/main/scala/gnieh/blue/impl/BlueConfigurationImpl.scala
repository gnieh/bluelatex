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
package impl

import java.util.Properties

import gnieh.sohva.sync._

import com.typesafe.config._

import scala.collection.JavaConverters._

class BlueConfigurationImpl(conf: Config) extends BlueConfiguration {

  // ===== blue core settings =====

  val blue = conf.getConfig("blue")

  val port = blue.getInt("port")

  val recaptchaPrivateKey = optionalString("recaptcha.private-key")

  // ===== synchronization server settings =====

  val synchro = conf.getConfig("synchro")

  // ===== couchdb settings =====

  val couchConf = conf.getConfig("couchdb")

  val couch = {
    val hostname = couchConf.getString("hostname")
    val port = couchConf.getInt("port")
    val ssl = couchConf.getBoolean("ssl")
    new CouchClient(host = hostname, port = port, ssl = ssl)
  }

  val couchAdminName = conf.getString("admin-name")

  val couchAdminPassword = conf.getString("admin-password")

  // ===== email settings =====

  val emailConf = {
    val props = new Properties
    for(entry <- conf.getConfig("mail").entrySet.asScala) {
      props.setProperty("mail." + entry.getKey, entry.getValue.render)
    }
    props
  }

  // ===== internals =====

  private def optionalString(path: String): Option[String] =
    if(conf.hasPath(path))
      Some(conf.getString(path))
    else
      None

}
