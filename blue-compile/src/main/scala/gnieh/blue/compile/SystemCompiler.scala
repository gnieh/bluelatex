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

import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Await

import scala.util.Try

import java.io.File

import com.typesafe.config.Config

import common.{
  PaperConfiguration,
  FileUtils
}

import scala.sys.process._
/** A LaTeX compiler that calls a compiler installed on the host system.
 *
 *  @author Lucas Satabin
 */
abstract class SystemCompiler(system: ActorSystem, config: Config, texmfcnf: File) extends Compiler {

  val configuration = new PaperConfiguration(config)

  private val systemCommand = system.actorSelection("/user/system-commands")

  protected def exec(command: String, workingDir: File, env: List[(String, String)] = List())(implicit timeout: Timeout) =
    Try {
      Await.result(
        systemCommand ? SystemCommand(command, workingDir, ("TEXMFCNF" -> s"${texmfcnf.getCanonicalPath}:") :: env, timeout) mapTo manifest[Int],
        timeout.duration
      ) == 0
    }

  def bibtex(paperId: String, settings: CompilerSettings)(implicit timeout: Timeout): Try[Boolean] = {
    // XXX this sucks! we need to copy the bib files to the build directory because bibtex
    // cannot handle compilation in a different directory correctly
    // technology from the 80's has limitations...
    // http://tex.stackexchange.com/questions/12686/how-do-i-run-bibtex-after-using-the-output-directory-flag-with-pdflatex-when-f
    // Also need to copy any bst style for thos to work. See #274
    import FileUtils._
    for(file <- configuration.paperDir(paperId).filter(f => List(".bib", ".bst").contains(f.extension))) {
      val destfile = configuration.buildDir(paperId) / file.getName
      (file #> destfile).!
    }

    exec(s"bibtex main.aux", configuration.buildDir(paperId))
  }

  def makeindex(paperId: String, settings: CompilerSettings)(implicit timeout: Timeout): Try[Boolean] = {
    exec(s"makeindex main.idx", configuration.buildDir(paperId))
  }

  protected def buildDir(paperId: String) = configuration.buildDir(paperId).getCanonicalPath
  protected def paperFile(paperId: String) = configuration.paperFile(paperId).getName

}

