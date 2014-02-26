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

import akka.actor.ActorRef

import http.RestApi
import common._
import let._

import com.typesafe.config.Config

import org.osgi.framework.BundleContext

/** The compilation feature Rest API that offers the services to compile
 *  papers
 *
 *  @author Lucas Satabin
 */
class CompilationApi(context: BundleContext, dispatcher: ActorRef, config: Config, logger: Logger) extends RestApi {

  POST {
    // join the paper compiler stream
    case p"papers/$paperid/compiler" =>
      new CompilerLet(paperid, dispatcher, config, logger)
  }

  PATCH {
    // saves the compilation settings
    case p"papers/$paperid/compiler" =>
      new ModifyCompilerLet(paperid, config, logger)
  }

  GET {
    // return the list of available compilers
    case p"compilers" =>
      new GetCompilersLet(context, config, logger)
    // return the compiled pdf file for the paper, if any
    case p"papers/$paperid/compiled/pdf" =>
      new GetPdfLet(paperid, config, logger)
    // return the last compilation log of any
    case p"papers/$paperid/compiled/log" =>
      new GetLogLet(paperid, config, logger)
    // return the page given as parameter converted as a png image
    case req @ p"papers/$paperid/compiled/png" =>
      val page = req.asInt("page").map(math.max(_, 1)).getOrElse(1)
      new GetPngLet(paperid, page, config, logger)
    // returns the number of pages in the compiled paper
    case p"papers/$paperid/compiled/pages" =>
      new GetPagesLet(paperid, config, logger)
    // return the compilation settings
    case p"papers/$paperid/compiler" =>
      new GetCompilerSettingsLet(paperid, config, logger)
    // return the SyncTeX file
    case p"papers/$paperid/synctex" =>
      new GetSyncTeXLet(paperid, config, logger)
  }

  DELETE {
    // cleanup the working directory
    case p"papers/$paperid/compiler" =>
      new CleanLet(paperid, config, logger)
  }

}
