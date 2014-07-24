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

import scala.language.postfixOps

import common._

import gnieh.sohva.control.Database

import scala.util.{
  Try,
  Success
}

import akka.actor.Props

import com.typesafe.config.Config

import org.osgi.framework.BundleContext
import org.osgi.service.log.LogService

import gnieh.sohva.control.CouchClient
import gnieh.sohva.control.entities.EntityManager

/** The compilation system actor is responsible for managing
 *  the compilation actor for each paper.
 *  Paper compilers are started either as bacgkround tasks or explicitly by authors depending
 *  on configuration.
 *
 *  @author Lucas Satabin
 */
class CompilationDispatcher(
  bndContext: BundleContext,
  couch: CouchClient,
  synchro: SynchroServer,
  config: Config,
  val logger: Logger
) extends ResourceDispatcher {

  private val background =
    config.getString("compiler.compilation-type") == "background"

  private val configuration = new PaperConfiguration(config)

  private val couchConf = new CouchConfiguration(config)

  import couchConf._

  def props(username: String, paperId: String) = asAdmin(couch) { session =>
    // get the compiler settings
    val db = session.database(database("blue_papers"))
    for(settings <- getOrCreateSettings(paperId, new EntityManager(db)))
      yield if(background)
        Props(new BackgroundCompilationActor(bndContext, synchro, config, paperId, settings, logger))
      else
        Props(new ExplicitCompilationActor(bndContext, synchro, config, paperId, settings, logger))

  }

  def getOrCreateSettings(paperId: String, manager: EntityManager): Try[CompilerSettings] =
    manager.getComponent[CompilerSettings](paperId).flatMap {
      case Some(settings) =>
        Success(settings)
      case None =>
        logger.log(LogService.LOG_WARNING, s"compiler settings do not exist for paper $paperId")
        // create the settings in the database and returns it
        // by default we compile with pdflatex with a timeout of 30 seconds and an interval of 15 seconds
        val settings = CompilerSettings(s"$paperId:compiler", "pdflatex", false, 30, 15)
        for(settings <- manager.saveComponent(paperId, settings))
          yield settings
    }

  override def unknownReceiver(paperId: String, msg: Any): Unit = msg match {
    case Register(_, client) =>
      // A client tried to registered to an unknown paper identifier,
      // to avoid having dangling request, reply immediately with an error
      client.complete(Try(CompilationAborted))
    case _ =>
      // ignore other messages
  }

}

