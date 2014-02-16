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
package compiler

import common._

import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent._

import scala.util.Try

import scala.sys.process._

import java.io.File

import com.typesafe.config.Config

/** Compiles a project with `pdflatex` and `bibtex`
 *
 *  @author Lucas Satabin
 */
class PdflatexCompiler(system: ActorSystem, config: Config) extends SystemCompiler(system) {

  val name: String = "pdflatex"

  val configuration = new PaperConfiguration(config)

  def compile(paperId: String, settings: CompilerSettings)(implicit timeout: Timeout): Try[Boolean] =
    exec(s"pdflatex -interaction nonstopmode -synctex=${if(settings.synctex) 1 else 0} -output-directory ${buildDir(paperId)} ${paperFile(paperId)}",
      configuration.paperDir(paperId)) //, List("TEXINPUT" -> ".:tex/:resources/:$TEXINPUTS"))

  def bibtex(paperId: String, settings: CompilerSettings)(implicit timeout: Timeout): Try[Boolean] = {
    // XXX this sucks! we need to copy the bib files to the build directory because bibtex
    // cannot handle compilation in a different directory correctly
    // technology from the 80's has limitations...
    // http://tex.stackexchange.com/questions/12686/how-do-i-run-bibtex-after-using-the-output-directory-flag-with-pdflatex-when-f
    import FileProcessing._
    for(file <- configuration.paperDir(paperId).filter(_.extension == ".bib")) {
      val destfile = configuration.buildDir(paperId) / file.getName
      (file #> destfile).!
    }

    exec(s"bibtex $paperId", configuration.buildDir(paperId))
  }

  private def buildDir(paperId: String) = configuration.buildDir(paperId).getCanonicalPath
  private def paperFile(paperId: String) = configuration.paperFile(paperId).getCanonicalPath

}

