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
package compile
package impl

import akka.actor.{
  Actor,
  ActorRef
}
import akka.util.Timeout

import scala.concurrent.Promise
import scala.concurrent.duration._

import scala.util.{
  Try,
  Success
}

import org.osgi.framework.BundleContext

import java.util.Date

import org.apache.pdfbox.pdmodel.PDDocument

import resource._

import common._

import couch.Paper

import gnieh.sohva.control._

/** This actor handles compilation of a paper.
 *  An instance of this actor is not eternally resident, if no
 *  compilation message was received within a session timeout,
 *  this actor is destroyed.
 *
 *  @author Lucas Satabin
 *
 */
class CompilationActor(
  bndContext: BundleContext,
  synchro: SynchroServer,
  configuration: PaperConfiguration,
  couchConfig: CouchConfiguration,
  paperId: String,
  defaultSettings: CompilerSettings,
  val logger: Logger
) extends Actor with Logging {

  import OsgiUtils._
  import FileUtils._

  @inline
  implicit def ec = context.system.dispatcher

  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce(Duration.Zero, self, Compile)
  }

  def receive = receiving(Map(), defaultSettings, None)

  def receiving(clients: Map[String, Promise[CompilationStatus]],
                settings: CompilerSettings,
                lastCompilationDate: Option[Date]): Receive = {
    case Compile =>

      implicit val timeout = Timeout(settings.timeout.seconds)

      // dump the files before printing
      synchro.persist(paperId)

      // get the last modification date
      val lastModificationDate = synchro.lastModificationDate(paperId)

      // create the build directory
      configuration.buildDir(paperId).mkdirs

      // Check if compilation is needed:
      // No need to recompile document if it has not been modified,
      // ie. last compilation date is the same as the last modification date.
      val hasBeenModified = (for {
        compilDate <- lastCompilationDate
      } yield compilDate before lastModificationDate).getOrElse(true)
      logDebug(s"Document needs to be compiled: $hasBeenModified")

      for {
        compiler <- bndContext.get[Compiler]
        couch <- bndContext.get[CouchClient]
      } {

        // TODO ideally the number of times we run the latex compiler, and bibtex,
        // and the index compiler should be smarter than this.
        // For the moment, we run only once, but we could make it configurable if the compilation
        // occurs on its own compilation server
        // it is also not necessary to run bibtex if no bibliography is to be generated
        // the settings should be able to handle this properly
        val res = if (!hasBeenModified) Success(CompilationUnnecessary) else for {
          // if the compiler is defined, we first compile the paper
          res <- compiler.compile(paperId, settings)
          // we run bibtex on it if the compilation succeeded
          _ <- compiler.bibtex(paperId, settings)
        } yield {
          // clean the generated png files when compilation succeeded
          for(file <- configuration.buildDir(paperId).filter(_.extension == ".png"))
            file.delete
          // extract the paper title and update it if necessary
          // i.e. if it changes since the last version saved in the database
          val pdfFile = configuration.buildDir(paperId) / s"$paperId.pdf"
          couchConfig.asAdmin(couch) { session =>
            val db = session.database(couchConfig.database("blue_papers"))
            for {
              paper <- db.getDocById[Paper](paperId)
              newTitle <- texTitle(paperId)
              newClass <- documentClass(paperId)
              p <- paper
              if p.title != newTitle || p.cls != newClass
            } db.saveDoc(p.copy(title = newTitle, cls = newClass).withRev(p._rev))
          }

          if(res)
            CompilationSucceeded
          else
            CompilationFailed

        }

        // and we send back the answer to the clients
        for((_, client) <- clients)
          client.complete(res)

        // if hasBeenModified is false, we are sure that lastCompilationDate is defined
        val newDate = if (hasBeenModified) lastModificationDate else lastCompilationDate.get

        // and listen again with an empty list of clients
        context.become(receiving(Map(), settings, Some(newDate)))

      }

      // schedule the next compilation after the configured interval
      context.system.scheduler.scheduleOnce(settings.interval.seconds, self, Compile)

    case settings @ CompilerSettings(_, _, _, _, _) =>
      // settings were changed, take them immediately into account
      context.become(receiving(clients, settings, lastCompilationDate))

    case Register(username, client) =>

      context.become(receiving(clients + (username -> client), settings, lastCompilationDate))

    case Part(username, _) =>

      context.become(receiving(clients - username, settings, lastCompilationDate))

    case Stop =>

      for((_, client) <- clients)
        client.complete(Try(CompilationAborted))

      context.become(receiving(Map(), settings, lastCompilationDate))

  }

  def texTitle(paperId: String) =
    configuration.paperFile(paperId)
      .extractFirst("\\\\title.([^}]+)".r)

  def documentClass(paperId: String) =
    configuration.paperFile(paperId)
      .extractFirst("""\\documentclass(?:\[[^\[]*\])?.([^}]+)""".r)

}

case object Compile
case class Register(username: String, response: Promise[CompilationStatus])

