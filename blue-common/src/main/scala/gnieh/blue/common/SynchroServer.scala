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

import scala.util.Try

import java.util.Date

/** Synchronization server interface
 *
 *  @author Lucas Satabin
 */
trait SynchroServer {

  /** Starts a new session with the data and returns
   *  the result data for the client
   */
  def session(data: String): Try[String]

  /** Persists the synchronized files for the given paper
   *  This call is synchronous and only returns when all files
   *  are synchronized
   */
  def persist(paperId: String): Unit

  /** Retrieve the last modification date of a paper.
   *  This date is updated every time a synchronized file of a paper
   *  (ie, associated to `paperId`) is updated.
   *  This call is synchronous.
   */
  def lastModificationDate(paperId: String): Date
}

/** Exception used in case of failure of the session() method.
 */
class SynchroFailureException(msg: String, inner: Throwable) extends Exception(msg, inner) {
  def this(msg: String) = this(msg, null)
}
