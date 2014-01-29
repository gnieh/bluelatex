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

import gnieh.sohva.control.CouchSession

trait PaperCreated {
  def afterCreate(paperId: String, session: CouchSession): Unit
}

trait PaperDeleted {
  def afterDelete(paperId: String, session: CouchSession): Unit
}

trait UserRegistered {
  def afterRegister(username: String, session: CouchSession): Unit
}

trait UserUnregistered {
  def afterUnregister(username: String, session: CouchSession): Unit
}

