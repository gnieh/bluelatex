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

import gnieh.sohva.sync._

import java.io.File

trait CouchConfiguration {

  /** The couchdb configuration if enabled */
  val couch: CouchClient

  /** The configuration directory that contains the designs */
  def designDir(dbName: String): File

  /** Executes some code with a session as adminstrator (automatically closed at the end) */
  def asAdmin[T](code: CouchSession => T): T

  /** Returns the configured name for the given database, or the name itself if it is not configured */
  def database(name: String): String

  /** The list of databases used by the core module */
  val databases: List[String]

}
