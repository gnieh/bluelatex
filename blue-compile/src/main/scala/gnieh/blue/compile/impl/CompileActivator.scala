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

import akka.actor._
import akka.routing.{
  DefaultResizer,
  RoundRobinRouter
}

import common.ConfigurationLoader

class CompileActivator extends BundleActivator {

  import common.OsgiUtils._

  def start(context: BundleContext) {
    for {
      loader <- context.get[ConfigurationLoader]
      system <- context.get[ActorSystem]
      logger <- context.get[LogService]
    } {
      val config = loader.load(context.getBundle.getSymbolicName)
      // create the dispatcher actor
      system.actorOf(Props(new CompilationDispatcher(context, config, logger)), name = "dispatcher")
      // create the system command actor
      system.actorOf(Props[SystemCommandActor]
          .withRouter(new RoundRobinRouter(
            DefaultResizer(config.getInt("tex.min-process"),
              config.getInt("tex.max-process")))), name = "system-commands")
    }
  }

  def stop(context: BundleContext) {
  }

}
