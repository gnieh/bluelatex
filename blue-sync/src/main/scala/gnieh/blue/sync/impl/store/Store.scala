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
package sync
package impl
package store

/** Describes the interface that all stores must implement.
 *  A store allows user to persist a document. It may be e.g. a file on the file system,
 *  a database, ...
 *
 *  @author Lucas Satabin
 */
trait Store {

  /** Persists the document in the store. Create it if it does not exist yet. */
  def save(document: Document)

  /** Loads the document from the store, if it does not exist, returns empty Document. */
  def load(documentId: String): Document

}

class StoreException(msg: String, inner: Exception) extends Exception(msg, inner) {
  def this(msg: String) = this(msg, null)
}
