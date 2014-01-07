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

import akka.util.Timeout

import scala.util.Try

import java.io.File

/** The `Compiler` interface provides a way for bundles to extend \BlueLaTeX with
 *  new ways of compiling a paper.
 *  Registering a new compiler is as simple as registering an OSGi service for this interface.
 *
 *  @author Lucas Satabin
 */
trait Compiler {

  /** The compiler name, used as key and for reference in the compiler settings.
   *  For example for `pdflatex` compiler the name is simply `pdflatex`.
   */
  val name: String

  /** Do the actual compilation of a paper, given the base directory
   *  containing the paper files.
   *  It returns the result of the compilation:
   *   - `true` is returned if the compilation was actually successfully performed,
   *   - `false` is returned if it was not performed
   *   - an error is returned if something went wrong
   */
  def compile(basedir: File)(implicit timeout: Timeout): Try[Boolean]

  /** Do the bibtex task for a paper, given the base directory
   *  containing the paper files.
   */
  def bibtex(basedir: File)(implicit timeout: Timeout): Try[Boolean]

}

