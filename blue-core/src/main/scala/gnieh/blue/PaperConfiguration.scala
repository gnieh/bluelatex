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

import com.typesafe.config.Config

import java.io.File

class PaperConfiguration(val config: Config) {

  import FileProcessing._

  def paperDir(paperId: String): File =
    new File(config.getString("blue.paper.directory")) / paperId

  def paperFile(paperId: String): File =
    paperDir(paperId) / s"$paperId.tex"

  def resource(paperId: String, resourceName: String): File =
    paperDir(paperId) / resourceName

  def bibFile(paperId: String): File =
    paperDir(paperId) / s"$paperId.bib"

  def clsDir: File =
    new File(config.getString("blue.paper.classes"))

  def clsFiles: List[File] =
    clsDir.filter(_.getName.endsWith(".cls"))

  def cls(name: String): File =
    clsDir / s"$name.cls"

}
