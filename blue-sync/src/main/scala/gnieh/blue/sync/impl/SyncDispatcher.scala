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

import scala.language.postfixOps

import util._

import couch.{
  Paper,
  User
}

import gnieh.sohva.sync.CouchClient

import akka.actor.{
  Actor,
  Props,
  ReceiveTimeout
}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

import java.io.{
  File,
  FilenameFilter
}

import scala.sys.process._

import com.typesafe.config.Config

import org.osgi.framework.BundleContext

/** The synchronization system actor is responsible for managing
 *  the synchronisation and persistance of papers.
 *
 *  @author Lucas Satabin
 */
class SyncDispatcher(bndContext: BundleContext, config: Config) extends Actor {

  val configuration = new PaperConfiguration(config)

  /* gets the synchronization actor for the file.
   */
  def forFile(file: String) =
    context.children.find(ref =>
      ref.path.name == file).getOrElse(
      // Actor for file does not exists, create it
      context.actorOf(Props(
        new SyncActor(bndContext, configuration, new File(file))),
        name = file))

  def receive =
    {
      case msg @ SyncSession(_, file, _, _) =>
        // simply forward to the paper actor
        forFile(file).forward(msg)
    }
}

/** This actor handles synchronisation of file.
 *  It is created upon reception of first ```Join``` system message,
 *  and should be destroyed when all corresponding ```Part``` messages
 *  have been received.
 *
 *  @author Audric Schiltknecht
 *
 */
class SyncActor(bndContext: BundleContext, configuration: PaperConfiguration, file: File) extends Actor with Logging {

  def receive = {
    case SyncSession(user, _, echo, commands) => sender ! (commands map {
      case x: Delta => processDelta(x)
      case x: Raw => processRaw(x)
      case Nullify => nullify()
      case x: Message => processMessage(x)
    })
  }

  def nullify(): String = {
    if (logger.isDebugEnabled)
      logger.debug(s"Delete file: $file")
    file.delete
    ""
  }

  def processDelta(delta: Delta): String = {
    ""
  }

  def processRaw(raw: Raw): String = {
    ""
  }

  def processMessage(msg: Message): String = {
    ""
  }


}

