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

import akka.actor.{
  Actor,
  Props
}
import scala.util.{Try, Success, Failure}
import com.typesafe.config.Config
import org.osgi.framework.BundleContext
import org.osgi.service.log.LogService
import name.fraser.neil.plaintext.DiffMatchPatch

import store.FsStore
import common._

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
