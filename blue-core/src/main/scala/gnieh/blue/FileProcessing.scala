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

import java.io.{
  File,
  FileFilter => JFileFilter,
  FilenameFilter => JNameFilter
}
import scala.util.matching.Regex
import Regex._
import resource._

/** @author Lucas Satabin
 *
 */
object FileProcessing {

  val synchronizedExt = """\.bib|\.tex"""

  val generatedExt = """\.aux|\.toc|\.bbl|\.log|\.out|\.blg"""

  /** Extends `java.io.File` with more scalaish features.
   *
   *  @author Lucas Satabin */
  implicit class RichFile(val file: File) extends AnyVal {

    /** Returns the list of file that respects the given predicate in this directory, or `Nil` if
     *  not a directory */
    def filter(f: File => Boolean): List[File] =
      if(file.isDirectory) {
        // apply filter to files in the directory
        file.listFiles(new FileFilter(f)).toList
      } else {
        Nil
      }

    /** Returns the list of file that respects the given predicate in this directory, or `Nil` if
     *  not a directory */
    def filter(f: (File, String) => Boolean): List[File] =
      if(file.isDirectory) {
        // apply filter to files in the directory
        file.listFiles(new NameFilter(f)).toList
      } else {
        Nil
      }

    @inline
    def /(name: String): File =
      new File(file, name)

    @inline
    def isHidden: Boolean =
      file.getCanonicalPath.startsWith(".")

    def isTeXTemporary: Boolean =
      extension.matches(generatedExt)

    def extension: String =
      if(file.isDirectory) {
        ""
      } else {
        val name = file.getCanonicalPath
        if(name.contains("."))
          name.substring(name.lastIndexOf('.'))
        else
          ""
      }

    def deleteRecursive(): Boolean = {
      def deleteFile(dfile: File): Boolean = {
        val ok = if (dfile.isDirectory) {
          val files = dfile.listFiles
          val oks = if (files != null)
            files.map(deleteFile).toList
          else
            Nil
          oks.forall(identity)
        } else {
          true
        }
        dfile.delete && ok
      }
      deleteFile(file)
    }

    def extractFirst(regex: Regex): Option[String] =
      regex.findFirstMatchIn(getFileContents).map(_.group(1))

    def extractAll(regex: Regex): List[String] =
      regex.findAllIn(getFileContents).toList

    private def getFileContents =
      (for(source <- managed(io.Source.fromFile(file)))
        yield source.mkString).opt.getOrElse("")

  }

  private class FileFilter(f: File => Boolean) extends JFileFilter {
    def accept(path: File) = f(path)
  }

  private class NameFilter(f: (File, String) => Boolean) extends JNameFilter {
    def accept(parent: File, name: String) = f(parent, name)
  }

}
