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

import http.RestApi

/** The compilation feature Rest API that offers the services to compile
 *  papers
 *
 *  @author Lucas Satabin
 */
class CompilationApi extends RestApi {

  POST {
    // compile the paper with the configured compiler
    case p"papers/$paperid/compiler" =>
      ???
    // saves the compilation settings
    case p"papers/$paperid/compiler/settings" =>
      ???
  }

  GET {
    // return the compiled pdf file for the paper, if any
    case p"papers/$paperid.pdf" =>
      ???
    // return the last compilation log of any
    case p"papers/$paperid.log" =>
      ???
    // return the page given as parameter converted as a png image
    case p"papers/$paperid.png" =>
      ???
    // return the compilation settings
    case p"papers/$paperid/compiler/settings" =>
      ???
  }

  DELETE {
    // cleanup the working directory
    case p"papers/$paperid/compiler" =>
      ???
  }

}
