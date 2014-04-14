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

import com.typesafe.config.Config

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import net.liftweb.json._
import net.liftweb.json.Serialization

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Try, Success, Failure}

import java.util.Date

/** A synchronization server.
 *
 * This synchronization server makes use of the mobwrite-based
 * Synchronization Protocol Scala implementation.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
class SyncServer(dispatcher: ActorRef, configuration: Config) extends SynchroServer {

  private implicit val timeout = Timeout(500 millis)

  protected[impl] implicit val formats =
    DefaultFormats +
    new SyncSessionSerializer +
    new SyncCommandSerializer +
    new SyncActionSerializer +
    new EditSerializer

  def session(data: String): Try[String] = {
    val syncSession = Serialization.read[SyncSession](data)
    val response = Try(Await.result(dispatcher ? Forward(syncSession.paperId, syncSession),
                                    timeout.duration).asInstanceOf[SyncSession])
    response match {
      case Success(resp) => Success(Serialization.write[SyncSession](resp))
      case Failure(e) =>
        Failure(new SynchroFailureException("Unable to get reponse from synchro dispatcher", e))
    }
  }

  def persist(paperId: String): Unit = {
    val promise = Promise[Unit]()

    dispatcher ! Forward(paperId, PersistPaper(promise))
    Await.result(promise.future, Duration.Inf)
  }

  def lastModificationDate(paperId: String): Date = {
    val promise = Promise[Date]

    dispatcher ! Forward(paperId, LastModificationDate(promise))
    Await.result(promise.future, Duration.Inf)
  }

  def shutdown(): Unit = {
    dispatcher ! Stop
  }
}
