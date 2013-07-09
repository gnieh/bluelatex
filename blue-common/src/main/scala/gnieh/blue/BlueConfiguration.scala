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

import java.util.Properties

import gnieh.sohva.sync._

import com.typesafe.config._

import java.io.File

/** The \BlueLaTeX server configuration interface that allows
 *  people to access the server settings (possibly personal settings)
 *
 *  @author Lucas Satabin
 */
trait BlueConfiguration {

  /** The server port */
  val port: Int

  /** The ReCaptcha private key if enabled */
  val recaptchaPrivateKey: Option[String]

  /** The synchronization server configuration */
  val synchro: Config

  /** The couchdb configuration keys */
  val couch: CouchConfiguration

  /** The email configuration */
  val emailConf: Properties

  /** The paper directory associated with the paper identifier */
  def paperDir(paperId: String): File

  /** The validity of a reset token in milliseconds */
  val resetTokenValidity: Long

  /** The directory where the templates are located */
  val templateDir: File

  /** The base Url of the \BlueLaTeX implementation */
  val baseUrl: String

}

