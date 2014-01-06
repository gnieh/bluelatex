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

import couch.{
  Paper,
  User
}
import common._

import gnieh.sohva.control.CouchClient

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
import org.osgi.service.log.LogService

/** The compilation system actor is responsible for managing
 *  the compilation actor for each paper
 *
 *  @author Lucas Satabin
 */
class CompilationDispatcher(bndContext: BundleContext, config: Config, val logger: LogService) extends ResourceDispatcher {

  val configuration = new CompileConfiguration(config)

  val couchConf = new CouchConfiguration(config)
  import couchConf._

  def props(username: String, paperId: String) = asAdmin { session =>
    // get the compiler settings
    val db = session.database(database("blue_papers"))
    for(settings <- db.getDocById[CompilerSettings](s"$paperId:compiler"))
      yield settings match {
        case Some(settings) =>
          Props(new CompilationActor(bndContext, configuration, paperId, settings, logger))
        case None =>
          // create the settings in the database and returns it
          ???
      }
  }

}

