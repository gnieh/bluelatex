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

import scala.collection.mutable._

/** User view of one Document
 *
 * @author Lucas Satabin
 * @author Audric Schiltknecht
 */
class DocumentView(val document: Document) {

  /** Last version of the text sent to the client */
  var shadow: String = ""
  /** Previous version of the text sent to the client */
  var backupShadow: String  = ""

  /** The client's version for the shadow (n) */
  var serverShadowRevision: Long = 0
  /** The server's version for the shadow (m) */
  var clientShadowRevision: Long = 0
  /** The server's version for the backup shadow */
  var backupShadowRevision: Long = 0

  /** List of unacknowledged edits sent to the client */
  var edits = ListBuffer.empty[(Long, SyncCommand)]

  /** Is the delta valid ? */
  var deltaOk = true
  /** Has the view changed since it was last saved ? */
  var changed = false

  /** Does the client set the overwrite flag ? */
  var overwrite = false

  def restoreBackupShadow(): Unit = {
    edits.clear()
    shadow = backupShadow
    serverShadowRevision = backupShadowRevision
  }

  def setShadow(text: String, clientRevision: Long, serverRevision: Long, force: Boolean): Unit = {
    deltaOk = true
    shadow = text
    clientShadowRevision = clientRevision
    serverShadowRevision = serverRevision
    backupShadow = shadow
    backupShadowRevision = serverShadowRevision
    edits.clear()

    if (force || document.text.isEmpty) {
      document.text = text
    }
    overwrite = force
  }

}
