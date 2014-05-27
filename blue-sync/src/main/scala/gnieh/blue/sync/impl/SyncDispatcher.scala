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

import scala.collection.mutable.Map
import scala.util.{Try, Success, Failure}
import com.typesafe.config.Config

import org.osgi.framework.BundleContext
import org.osgi.service.log.LogService

import java.net.URLEncoder;
import java.net.URLDecoder;

import java.util.{Date, Calendar}

import name.fraser.neil.plaintext.DiffMatchPatch

import gnieh.blue.sync.impl.store._

/** The synchronization system actor is responsible for managing
 *  the synchronisation and persistance of papers.
 *
 *  @author Lucas Satabin
 */
class SyncDispatcher(bndContext: BundleContext, config: Config, val logger: LogService) extends ResourceDispatcher {

  private val configuration = new PaperConfiguration(config)
  private val dmp = new DiffMatchPatch
  private val store = new FsStore

  def props(username: String, paperId: String): Try[Props] =
    Try(Props(new SyncActor(configuration, paperId, store, dmp, logger)))

  override def unknownReceiver(paperId: String, msg: Any): Unit = msg match {
    case SyncSession(peerId, paperId, commands) =>
      sender ! akka.actor.Status.Failure(new SynchroFailureException(s"Nobody is connected to paper $paperId"))

    case PersistPaper(promise) =>
      promise.complete(Failure(new SynchroFailureException(s"Nobody is connected to paper $paperId")))

    case LastModificationDate(promise) =>
      promise.complete(Failure(new SynchroFailureException(s"Nobody is connected to paper $paperId")))

  }

}

/** This actor handles synchronisation of documents.
 *  It is created upon reception of first ```Join``` system message,
 *  and should be destroyed when all corresponding ```Part``` messages
 *  have been received.
 *
 *  @author Audric Schiltknecht
 */
class SyncActor(
    config: PaperConfiguration,
    paperId: String,
    store: Store,
    dmp: DiffMatchPatch,
    val logger: LogService)
  extends Actor
  with Logging {

  import FileUtils._
  import scala.collection.mutable.ListBuffer
  import net.liftweb.json.JObject


  private val paperDir = config.paperDir(paperId)

  /** Map (peer, filepath) -> DocumentView instance */
  private var views = Map.empty[(String, String), DocumentView]
  /** Map filepath -> Document instance */
  private val documents = Map.empty[String, Document]
  /** Map destPeer -> List(message) */
  private val messages = Map.empty[String, ListBuffer[Message]]

  /** Store the last modification time for this paperId */
  private var lastModificationTime = Calendar.getInstance().getTime()

  override def postStop() {
    persistPapers()
  }

  private def updateLastModificationTime(): Unit =
      lastModificationTime = Calendar.getInstance().getTime()

  def receive = {
    case Join(peerId, _) =>
      logInfo(s"peer $peerId joined paper $paperId")
      messages += (peerId -> ListBuffer.empty[Message])
    case Part(peerId, _) => {
      logInfo(s"peer $peerId left paper $paperId")
      for {
        (peer, filepath) <- views.keys
        if peer == peerId
      } views.remove((peerId, filepath))
      messages.remove(peerId)
    }
    case SyncSession(peerId, paperId, commands) => {
      Try {
        val commandResponse = commands flatMap {
          case message: Message => {
            processMessage(peerId, message)
            Nil
          }
          case SyncCommand(filename, revision, action) => {
            applyAction(peerId, filename, revision, action)
          }
        }
        val bcastMessages = retrieveMessages(peerId, paperId)
        sender ! SyncSession(peerId, paperId, commandResponse ++ bcastMessages)
      } recover {
        case e: Exception =>
          logError(s"Error while processing synchronization from peer $peerId", e)
          sender ! akka.actor.Status.Failure(e)
      }
    }
    case PersistPaper(promise) => {
      promise.complete(persistPapers())
    }

    case LastModificationDate(promise) => {
      promise.success(lastModificationTime)
    }
  }

  def persistPapers(): Try[Unit] =
    Try {
      for {
        doc <- documents.values
      } store.save(doc)
    }

  def applyAction(peer: String,
                  filename: String,
                  revision: Long,
                  action: SyncAction): List[SyncCommand] = {
    val filepath = (paperDir / filename).getCanonicalPath
    // Load document and associated view
    val document = documents.getOrElse(filepath, store.load(filepath))
    val view = views.getOrElse((peer, filepath), new DocumentView(document))
    if (!documents.contains(filepath))
      documents += (filepath -> document)
    if (!views.contains((peer, filepath)))
      views += ((peer, filepath) -> view)

    logDebug(s"Apply SyncAction: peer=$peer, filename=$filepath, revision=$revision, action=$action")
    logDebug(s"Server shadow revision=${view.serverShadowRevision}, backup shadow revision=${view.backupShadowRevision}")

    // Start by checking revision
    if ((revision != view.serverShadowRevision)
        && (revision == view.backupShadowRevision)) {
      // If the version number does not equal the version number of the shadow,
      // but does equal the version number of the backup shadow, then
      // the previous response was lost, which means the backup shadow
      // and its version number should be copied over to the shadow (step 4)
      // and the local stack should be cleared. Continue.
      logDebug("Restore backup shadow")
      view.restoreBackupShadow()
      updateLastModificationTime()
    }

    // If the version number is equal to one of the edits on the local stack,
    // then this is an acknowledgment of receipt of those edits, which
    // means that edit and those with smaller version numbers should be dropped.
    // Continue.
    view.edits = view.edits.filter {
      case SyncCommand(_, editRev, _) => (editRev > revision)
    }

    if (revision != view.serverShadowRevision) {
      // If the version number is not equal the version number of the shadow,
      // then this means there is a memory/programming/transmission
      // bug so the system should be reinitialized using a 'raw' command.
      // Do not accept any Delta commands for this file
      logWarn(s"Error: revision ($revision) != view.serverShadowRevision (${view.serverShadowRevision}) for file $filepath")
      view.deltaOk = false
    } else if (revision == view.serverShadowRevision) {
      // The version number matches the shadow, proceed.
      view.deltaOk == true
    }

    action match {
      case x: Delta => processDelta(view, x, revision)
      case x: Raw => processRaw(view, x, revision)
      case Nullify => nullify(view)
    }
    var response = flushStack(view)
    response map ( SyncCommand(filename, view.serverShadowRevision, _) )
  }

  def nullify(view: DocumentView): Unit = {
    // remove document from memory
    // only keep views on documents that are not the gone we nullify
    views = views.filterNot { case ((_, path), _) => path == view.document.path }
    documents -= view.document.path
    // delete it from store
    store.delete(view.document)
  }

  def processDelta(view: DocumentView, delta: Delta, serverRevision: Long): Unit = {
    logDebug(s"Process delta command=$delta, for serverRevision=$serverRevision")

    if (!view.deltaOk) {
      logDebug("Invalid delta, abort")
      return
    }
    if (serverRevision < view.serverShadowRevision) {
      // Ignore delta on mismatched server shadow
      logDebug("Mismatched server shadow, ignore")
    } else if (delta.revision > view.clientShadowRevision) {
      // System should be re-initialised with Raw command
      view.deltaOk = false
      logDebug("Error: wait for Raw command")
    } else if (delta.revision < view.clientShadowRevision) {
      // Already seen it, drop
      logDebug("Delta already processed, drop")
    } else {
      logDebug("Delta and revision ok, process patches")
      applyPatches(view, delta)
      view.clientShadowRevision += 1
      view.update()
    }
    view.overwrite = delta.overwrite
  }

  def processRaw(view: DocumentView, raw: Raw, serverRevision: Long): Unit = {
    // Decode HTML-encoded data
    val data = URLDecoder.decode(raw.data, "UTF-8")
    view.setShadow(data, raw.revision, serverRevision, raw.overwrite)
    updateLastModificationTime()
  }

  def processMessage(peer: String, message: Message): Unit = {
    logDebug(s"Process message: $message")

    for {
      p <- messages.keys
      if (p != peer)
    } messages(p).append(message)
  }

  def retrieveMessages(peer: String, paperId: String): List[Message] =
    messages.remove(peer) match {
        case None => Nil
        case Some(m) => m.toList
    }

  def applyPatches(view: DocumentView, delta: Delta): Unit = {
    // Compute diffs
    // XXX: not very Scala-ish...
    val diffs = Try(dmp.diff_fromDelta(view.shadow, delta.data.mkString("\t"))) match {
      case Success(s) => s
      case Failure(e) => {
        logWarn(s"Delta failure, expected length ${view.shadow.length}")
        view.deltaOk = false
        null
      }
    }
    if (diffs == null)
      return

    // Expand diffs into patch
    val patch = dmp.patch_make(view.shadow, diffs)

    // Update client shadow first
    view.shadow = dmp.diff_text2(diffs)
    view.backupShadow = view.shadow
    view.backupShadowRevision = view.serverShadowRevision
    view.changed = true

    logDebug(s"Old document text: ${view.document.text}")

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
    logDebug(s"New document text: $mastertext")
    view.document.text = mastertext

    if (!patch.isEmpty) {
      logDebug("Patch is not empty -> update modification time")
      updateLastModificationTime()
    }
  }

  def flushStack(view: DocumentView): List[SyncAction] = {
    logDebug("Flush stack")

    val doc = view.document
    val mastertext = doc.text
    val filename = doc.filename

    if (view.deltaOk) {
      logDebug(s"Deltas OK, compute and apply diff on mastertext=$mastertext")
      // compute the diffs with the current master text
      val diffs = dmp.diff_main(view.shadow, mastertext)
      dmp.diff_cleanupEfficiency(diffs)
      // apply the diffs to the text
      val text = dmp.diff_toDelta(diffs)

      // parse back the computed delta texts
      val edits = EditCommandParsers.parseEdits(text)

      logDebug(s"Computed deltas: $edits")

      if (view.overwrite) {
        // Client sending 'D' means number, no error.
        // Client sending 'R' means number, client error.
        // Both cases involve numbers, so send back an overwrite delta.
        view.edits.append(SyncCommand(filename,
                                      view.serverShadowRevision,
                                      Delta(view.serverShadowRevision,
                                            edits,
                                            true)))
      } else {
        // Client sending 'D' means number, no error.
        // Client sending 'R' means number, client error.
        // Both cases involve numbers, so send back a merge delta.
        view.edits.append(SyncCommand(filename,
                                      view.serverShadowRevision,
                                      Delta(view.serverShadowRevision,
                                            edits,
                                            false)))
      }
      view.serverShadowRevision += 1
    } else {
      // Error server could not parse client's delta.
      logDebug("Invalid delta(s)")
      // Send a raw dump of the text.
      view.clientShadowRevision += 1

      if (mastertext.isEmpty) {
        view.edits.append(SyncCommand(filename,
                                      view.serverShadowRevision,
                                      Raw(view.serverShadowRevision,
                                          "",
                                          false)))
      } else {
        // Force overwrite client
        val text = encodeUriCompatibility(mastertext)
        view.edits.append(SyncCommand(filename,
                                      view.serverShadowRevision,
                                      Raw(view.serverShadowRevision,
                                          text,
                                          true)))
      }
    }

    view.shadow = mastertext
    view.changed = true

    view.edits.toList.map {
      case SyncCommand(_, _, command)  => command
    }
  }

  /**
   * Unescape selected chars for compatibility with JavaScript's encodeURI.
   * Taken from "Diff Match Patch" java's implementation by Neil Fraser.
   * @param str The string to escape.
   * @return The escaped string.
   */
  private def encodeUriCompatibility(str: String): String = {
    return URLEncoder.encode(str, "UTF-8")
        .replace('+', ' ').replace("%21", "!").replace("%7E", "~")
        .replace("%27", "'").replace("%28", "(").replace("%29", ")")
        .replace("%3B", ";").replace("%2F", "/").replace("%3F", "?")
        .replace("%3A", ":").replace("%40", "@").replace("%26", "&")
        .replace("%3D", "=").replace("%2B", "+").replace("%24", "$")
        .replace("%2C", ",").replace("%23", "#")
  }

}
