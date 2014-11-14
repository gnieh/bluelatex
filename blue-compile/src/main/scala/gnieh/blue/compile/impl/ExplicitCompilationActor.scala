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
import gnieh.sohva.control.entities.EntityManager

import com.typesafe.config.Config

/** This actor handles compilation of a paper.
 *  It handles only explicit compilation requests.
 *
 *  @author Lucas Satabin
 *
 */
class ExplicitCompilationActor(
  bndContext: BundleContext,
  synchro: SynchroServer,
  configuration: Config,
  paperId: String,
  defaultSettings: CompilerSettings,
  val logger: Logger
) extends Actor with Logging {

  import OsgiUtils._
  import FileUtils._

  val paperConfig = new PaperConfiguration(configuration)

  @inline
  implicit def ec = context.system.dispatcher

  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce(Duration.Zero, self, Compile)
  }

  def receive = receiving(defaultSettings, None)

  def receiving(settings: CompilerSettings,
                lastCompilationDate: Option[Date]): Receive = {
    case Register(_, client) =>

      implicit val timeout = Timeout(settings.timeout.seconds)

      // dump the files before printing
      synchro.persist(paperId)

      // get the last modification date
      val lastModificationDate = synchro.lastModificationDate(paperId)

      // create the build directory
      val buildDir = paperConfig.buildDir(paperId)
      buildDir.mkdirs

      // Check if compilation is needed:
      // No need to recompile document if it has not been modified,
      // ie. last compilation date is the same as the last modification date.
      val hasBeenModified = (for {
        compilDate <- lastCompilationDate
      } yield compilDate before lastModificationDate).getOrElse(true)
      logDebug(s"Document needs to be compiled: $hasBeenModified")

      for {
        compiler <- bndContext.get[Compiler]("name" -> settings.compiler)
        couch <- bndContext.get[CouchClient]
      } {

        // TODO ideally the number of times we run the latex compiler, and bibtex,
        // and the index compiler should be smarter than this.
        // For the moment, we run only once, but we could make it configurable if the compilation
        // occurs on its own compilation server
        // it is also not necessary to run bibtex if no bibliography is to be generated
        // the settings should be able to handle this properly
        val res = if (!hasBeenModified && (buildDir / "main.aux").exists) Success(CompilationUnnecessary) else for {
          // if the compiler is defined, we first compile the paper
          res <- compiler.compile(paperId, settings)
          // we run bibtex on it if the compilation succeeded
          _ <- compiler.bibtex(paperId, settings)
        } yield {
          // clean the generated png files when compilation succeeded
          for(file <- paperConfig.buildDir(paperId).filter(_.extension == ".png"))
            file.delete

          if(res)
            CompilationSucceeded
          else
            CompilationFailed

        }

        // and we send back the answer to the client
        client.complete(res)

        // if hasBeenModified is false, we are sure that lastCompilationDate is defined
        val newDate = if (hasBeenModified) Some(lastModificationDate) else lastCompilationDate

        // and listen again with an empty list of clients
        context.become(receiving(settings, newDate))

      }

    case settings @ CompilerSettings(_, _, _, _, _) =>
      // settings were changed, take them immediately into account
      context.become(receiving(settings, lastCompilationDate))

    case Stop =>

      context.become(stopping)

  }

  val stopping: Receive = {

    case Register(_, client) =>

      client.complete(Try(CompilationAborted))

  }

}
