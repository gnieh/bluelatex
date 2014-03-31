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

import scala.util.{Try, Success, Failure}

import resource._

/** Stores documents as files on the file system.
 *
 *  @author Lucas Satabin
 */
class FsStore extends Store {

  def save(document: Document): Unit = {
    val file = new File(document.path)
    // File will be created iif it does not exist, so do not bother to check
    Try(file.createNewFile()) match {
      case Failure(e) => throw new StoreException(s"Cannot create file ${document.path}", e)
      case _ =>
    }

    // write to file system
    for(writer <- managed(new BufferedWriter(new FileWriter(file)))) {
      writer.write(document.text)
      writer.flush()
    }
  }

  def load(documentPath: String): Document = {
    val file = new File(documentPath)
    if (file.exists) {
      managed(Source.fromFile(file)).acquireAndGet { source =>
        new Document(documentPath, source.mkString)
      }
    } else {
      val doc = new Document(documentPath, "")
      save(doc)
      doc
    }
  }

  def delete(document: Document): Unit = {
    val file = new File(document.path)
    // Same here, nothing done if file does not exist
    Try(file.delete()) match {
      case Failure(e) => throw new StoreException(s"Cannot delete file ${document.path}", e)
      case _ =>
    }
  }

}
