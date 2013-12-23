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
package couch

import resource._

import common._

import gnieh.sohva.control._

import net.liftweb.json._

import scala.io._

import org.osgi.service.log.LogService

/** A design manager is able to check new design document and
 *  update existing design documents for a given database.
 *
 *  @author Lucas Satabin
 *
 */
class DesignManager(configuration: CouchConfiguration, db: Database, val logger: LogService) extends Logging {

  lazy val dir = configuration.designDir(db.name)

  /** Loads all the design files from the file system, compare their version
   *  with the existing one, and update/create if necessary
   */
  def reload: Unit = {
    if (dir.exists && dir.isDirectory) {
      // only reload if the directory exists
      dir.listFiles.foreach { file =>
        // try to parse the file to a design document
        for(source <- managed(Source.fromFile(file))) {
          try {
            implicit val formats = DefaultFormats
            val json = JsonParser.parse(source.mkString)
            Extraction.extractOpt[DesignDoc](json) match {
              case Some(design) =>
                logInfo("Save design " + design._id)
                db.saveDoc(design)
              case None => // not a design ignore it
            }
          } catch {
            case e: Exception =>
              logError(s"Error while parsing file ${file.getCanonicalPath}", e)
          }

        }
      }
    }
  }

}

case class DesignDoc(_id: String,
                     language: String,
                     views: Map[String, ViewDoc],
                     validate_doc_update: Option[String])(
                       val _rev: Option[String] = None)

case class ViewDoc(map: String,
                   reduce: Option[String])
