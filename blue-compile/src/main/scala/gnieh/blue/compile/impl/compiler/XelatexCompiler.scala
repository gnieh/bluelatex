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

import java.io.File

import com.typesafe.config.Config

/** Compiles a project with `xelatex` and `bibtex`
 *
 *  @author Lucas Satabin
 */
class XelatexCompiler(system: ActorSystem, config: Config, configDir: File) extends SystemCompiler(system, config, configDir) {

  val name: String = "xelatex"

  def compile(paperId: String, settings: CompilerSettings)(implicit timeout: Timeout): Try[Boolean] =
    exec(s"xelatex -interaction nonstopmode -synctex=${if(settings.synctex) 1 else 0} -output-directory ${buildDir(paperId)} ${paperFile(paperId)}",
      configuration.paperDir(paperId)) //, List("TEXINPUT" -> ".:tex/:resources/:$TEXINPUTS"))

}
