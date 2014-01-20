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

import common.SynchroServer

import com.typesafe.config.Config

import akka.actor.{ActorRef, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout

import net.liftweb.json._
import net.liftweb.json.Serialization

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Try, Success, Failure}

/** A synchronization server.
 *
 * This synchronization server makes use of the mobwrite-based
 * Synchronization Protocol Scala implementation.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
class SyncServer(dispatcher: ActorRef, configuration: Config) extends SynchroServer {

  private implicit val formats = DefaultFormats +
                                 new SyncSessionSerializer +
                                 new SyncCommandSerializer +
                                 new SyncActionSerializer +
                                 new EditSerializer

  private implicit val timeout = Timeout(500 millis)

  def session(data: String): String = {
    val syncSession = Serialization.read[SyncSession](data)
    val response = Try(Await.result(dispatcher ? Forward(syncSession.paperId, syncSession),
                                    timeout.duration).asInstanceOf[SyncSession])
    response match {
      case Success(resp) => Serialization.write[SyncSession](resp)
      case Failure(e) => e.getMessage //TODO: Return specific HTTP error ?
    }
  }

  def persist(paperId: String): Unit = {
    dispatcher ! Forward(paperId, PersistPaper)
  }

  def shutdown(): Unit = {
    dispatcher ! PoisonPill
  }
}
