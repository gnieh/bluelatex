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

import org.osgi.framework._
import org.osgi.service.log.LogService

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

import scala.sys.process.Process

import akka.actor._
import akka.routing.{
  DefaultResizer,
  RoundRobinPool
}

import common._
import compiler._

import http.RestApi

import gnieh.sohva.control.CouchClient

class CompilationActivator extends BundleActivator {

  self =>

  import OsgiUtils._

  private var dispatcher: Option[ActorRef] = None
  private var commands: Option[ActorRef] = None
  private val services = ListBuffer.empty[ServiceRegistration[_]]

  def start(context: BundleContext): Unit =
    context.trackOne[SynchroServer] {
      case ServiceAdded(synchro) => deploy(context, synchro)
      case ServiceRemoved(_)     => undeploy(context)
    }

  def stop(context: BundleContext): Unit =
    undeploy(context)

  def deploy(context: BundleContext, synchro: SynchroServer): Unit =
    for {
      loader <- context.get[ConfigurationLoader]
      system <- context.get[ActorSystem]
      couch <- context.get[CouchClient]
      logger <- context.get[LogService]
    } {
      val config = loader.load(context.getBundle)

      // create the dispatcher actor
      val disp =
        system.actorOf(
          Props(new CompilationDispatcher(context, couch, synchro, config, logger)),
          name = "compilation-dispatcher"
        )
      dispatcher = Option(disp)
      // create the system command actor
      val comm =
        system.actorOf(
          RoundRobinPool(
            0,
            Some(DefaultResizer(
              config.getInt("tex.min-process"),
              config.getInt("tex.max-process")
            ))
          ).props(Props(new SystemCommandActor(logger))),
          name = "system-commands"
        )
      commands = Option(comm)

      // register the compilation Api
      services +=
        context.registerService(classOf[RestApi], new CompilationApi(context, couch, disp, config, logger), null)

      // register the paper services
      services +=
        context.registerService(classOf[PaperCreated], new CreateSettingsHook(config, logger), null)

      // register the compiler services
      registerCompiler(context, new PdflatexCompiler(system, config, loader.base))
      registerCompiler(context, new LatexCompiler(system, config, loader.base))
      registerCompiler(context, new XelatexCompiler(system, config, loader.base))
      registerCompiler(context, new LualatexCompiler(system, config, loader.base))

    }

  def registerCompiler(context: BundleContext, compiler: Compiler): Unit = {
    services +=
      context.registerService(classOf[Compiler], compiler, collection.mutable.Map("name" -> compiler.name).asJavaDictionary)
  }

  def undeploy(context: BundleContext): Unit = {
    dispatcher.foreach(_ ! Stop)
    commands.foreach(_ ! PoisonPill)
    // unregister services
    services.foreach(_.unregister())

    dispatcher = None
    commands = None
    services.clear
  }

}
