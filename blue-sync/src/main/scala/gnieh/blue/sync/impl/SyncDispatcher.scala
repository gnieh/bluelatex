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

import common._

import akka.actor.{
  Actor,
  Props
}

import scala.collection.mutable.HashMap;

import com.typesafe.config.Config

import org.osgi.framework.BundleContext

import gnieh.blue.sync.impl.store._

import name.fraser.neil.plaintext.diff_match_patch

/** The synchronization system actor is responsible for managing
 *  the synchronisation and persistance of papers.
 *
 *  @author Lucas Satabin
 */
class SyncDispatcher(bndContext: BundleContext, config: Config) extends ResourceDispatcher {

  val configuration = new PaperConfiguration(config)

  def props(username: String, resourceid: String): Props =
    Props(new SyncActor(bndContext, configuration, resourceid))
}

/** This actor handles synchronisation of documents.
 *  It is created upon reception of first ```Join``` system message,
 *  and should be destroyed when all corresponding ```Part``` messages
 *  have been received.
 *
 *  @author Audric Schiltknecht
 */
class SyncActor(
    bndContext: BundleContext,
    config: PaperConfiguration,
    documentPath: String)
  extends Actor
  with Logging {

  private val dmp = new diff_match_patch

  private val store = new FsStore
  private val document = store.load(documentPath)

  private var views = HashMap.empty[String, DocumentView]

  def receive = {
    case SyncSession(user, _, echo, revision, commands) => {
      val view = views.getOrElse(user, new DocumentView(document))

      // Start by checking revision
      if ((revision != view.serverShadowRevision)
          && (revision == view.backupShadow)) {
        // If the version number does not equal the version number of the shadow,
        // but does equal the version number of the backup shadow, then
        // the previous response was lost, which means the backup shadow
        // and its version number should be copied over to the shadow (step 4)
        // and the local stack should be cleared. Continue.
        view.restoreBackupShadow()
      }

      // If the version number is equal to one of the edits on the local stack,
      // then this is an acknowledgment of receipt of those edits, which
      // means that edit and those with smaller version numbers should be dropped.
      // Continue.
      view.edits = view.edits.filter {
        case (editRev, _) => (editRev > revision)
      }

      if (revision != view.serverShadowRevision) {
        // If the version number is not equal the version number of the shadow,
        // then this means there is a memory/programming/transmission
        // bug so the system should be reinitialized using a 'raw' command.
        // Do not accept any Delta commands for this file
        view.deltaOk = false
      } else if (revision == view.serverShadowRevision) {
        // The version number matches the shadow, proceed.
        view.deltaOk == true
      }

      commands foreach {
        case x: Delta => processDelta(view, x, revision)
        case x: Raw => processRaw(view, x, revision)
        case Nullify => nullify(view)
        case x: Message => processMessage(view, x)
      }
      val result = flushStack(view)
      sender ! SyncSession(user, documentPath, false, view.serverShadowRevision, result)
    }
  }

  def nullify(view: DocumentView): Unit = {
    store.delete(view.document)
  }

  def processDelta(view: DocumentView, delta: Delta, serverRevision: Long): Unit = {
    if (!view.deltaOk)
      return
    if (serverRevision < view.serverShadowRevision) {
      // Ignore delta on mismatched server shadow
    } else if (delta.revision > view.clientShadowRevision) {
      // System should be re-initialised with Raw command
      view.deltaOk = false
    } else if (delta.revision < view.clientShadowRevision) {
      // Already seen it, drop
    } else {
      applyPatches(view, delta)
      view.clientShadowRevision += 1
    }
    view.overwrite = delta.overwrite
  }

  def processRaw(view: DocumentView, raw: Raw, serverRevision: Long): Unit = {
    view.setShadow(raw.data, raw.clientRevision, serverRevision, raw.overwrite)
  }

  def processMessage(view: DocumentView, msg: Message): Unit = ???


  def applyPatches(view: DocumentView, delta: Delta): Unit = {
    // Compute diffs
    val diffs = dmp.diff_fromDelta(view.shadow, delta.data.mkString("\t"))
    // Expand diffs into patch
    val patch = dmp.patch_make(view.shadow, diffs)

    // Update client shadow first
    view.shadow = dmp.diff_text2(diffs)
    view.backupShadow = view.shadow
    view.backupShadowRevision = view.serverShadowRevision
    view.changed = true

    // Update server document
    val mastertext = if (delta.overwrite) {
      if (patch != null) {
        view.shadow
      } else {
        view.document.text
      }
    } else {
      val Array(text: String, _) = dmp.patch_apply(patch, view.document.text)
      text
    }
    view.document.text = mastertext
  }

  def flushStack(view: DocumentView): List[SyncCommand] = {
    val doc = view.document
    val mastertext = doc.text

    if (view.deltaOk) {
      // compute the diffs with the current master text
      val diffs = dmp.diff_main(view.shadow, mastertext)
      dmp.diff_cleanupEfficiency(diffs)
      // apply the diffs to the text
      val text = dmp.diff_toDelta(diffs)

      if (view.overwrite) {
        // Client sending 'D' means number, no error.
        // Client sending 'R' means number, client error.
        // Both cases involve numbers, so send back an overwrite delta.
        view.edits.append((view.serverShadowRevision,
                           Delta(view.serverShadowRevision,
                                 EditCommandParsers.parseEdits(text), true)))
      } else {
        // Client sending 'D' means number, no error.
        // Client sending 'R' means number, client error.
        // Both cases involve numbers, so send back an overwrite delta.
        view.edits.append((view.serverShadowRevision,
                           Delta(view.serverShadowRevision,
                                 EditCommandParsers.parseEdits(text), false)))
      }
      view.serverShadowRevision += 1
    } else {
      // Error; server could not parse client's delta.
      // Send a raw dump of the text.
      view.clientShadowRevision += 1

      if (mastertext.isEmpty) {
        view.edits.append((view.serverShadowRevision,
                           Raw(view.serverShadowRevision, "", false)))
      } else {
        // Force overwrite client
        //          val text = URLEncoder.encode(mastertext, "UTF-8")
        view.edits.append((view.serverShadowRevision,
                           Raw(view.serverShadowRevision, mastertext, true)))
      }
    }

    view.shadow = mastertext
    view.changed = true

    view.edits.toList.map{
      case (_, command)  => command
    }
  }

}
