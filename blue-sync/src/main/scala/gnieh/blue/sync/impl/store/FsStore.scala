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
package sync
package impl
package store

import scala.io._
import java.io._

import scala.language.postfixOps;

import resource._

/** Stores documents as files on the file system.
 *
 *  @author Lucas Satabin
 */
class FsStore extends Store {

  def save(document: Document) {
    val file = new File(document.path)

    if (!file.exists)
      file.createNewFile

    // write to file system
    managed(new BufferedWriter(new FileWriter(file))).map { writer =>
      writer.write(document.text)
    }

  }

  def load(documentPath: String): Document = {
    val file = new File(documentPath)

    if (file.exists) {
      managed(Source.fromFile(file)).map { source =>
        new Document(documentPath, source.mkString)
      } now
    } else {
      new Document(documentPath, "")
    }
  }

}
