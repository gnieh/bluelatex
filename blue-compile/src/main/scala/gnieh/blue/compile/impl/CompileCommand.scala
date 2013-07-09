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

import akka.util.Timeout

import couch.User

/** @author Lucas Satabin
 *
 */
sealed trait CompileCommand {
  val paperId: String
  val density: Int
  val timeout: Timeout
  val user: Option[User]
}
case class PdfLatex(paperId: String, density: Int, timeout: Timeout, user: Option[User]) extends CompileCommand
case class Latex(paperId: String, density: Int, timeout: Timeout, user: Option[User]) extends CompileCommand
object CompileCommand {

  def apply(str: String, paperId: String, density: Int, timeout: Timeout, user: Option[User]) = str match {
    case "latex" => Latex(paperId, density, timeout, user)
    case "pdflatex" => PdfLatex(paperId, density, timeout, user)
  }

}
