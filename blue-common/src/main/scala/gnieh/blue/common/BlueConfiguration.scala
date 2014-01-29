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

import java.util.Properties

import gnieh.sohva.control._

import com.typesafe.config._

import scala.collection.JavaConverters._

import java.io.{
  File,
  FileNotFoundException
}

class BlueConfiguration(config: Config) extends PaperConfiguration(config) {

  // ===== blue core settings =====

  val blue = config.getConfig("blue")

  val recaptchaPrivateKey = optionalString("recaptcha.private-key")

  // ===== couchdb settings =====

  val couch = new CouchConfiguration(config)

  // ===== email settings =====

  val emailConf = {
    val props = new Properties
    for(entry <- config.getConfig("mail").entrySet.asScala) {
      props.setProperty("mail." + entry.getKey, entry.getValue.unwrapped.toString)
    }
    props
  }

  // ===== templates =====

  val templateDir =
    new File(config.getString("blue.template.directory"))

  // ===== internals =====

  private def optionalString(path: String): Option[String] =
    if(config.hasPath(path))
      Some(config.getString(path))
    else
      None

}
