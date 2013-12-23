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

import common.CouchConfiguration

/** the database manager is in charge of database creation and designs management.
 *
 *  @author Lucas Satabin
 */
class DbManager(configuration: CouchConfiguration) extends Logging {

  /** Starts the database manager.
   *  It creates the databases that need to be created and update all
   *  needed design documents
   */
  def start() {
    configuration.asAdmin { session =>
      configuration.databases.foreach { db =>
        try {
          // first create inexistent databases
          val database = session.database(db, credit = 1)
          database.create
          // then create the design manager and update design documents
          val dm = new DesignManager(configuration, database)
          dm.reload
        } catch {
          case e: Exception => // ignore exception
        }
      }
    }
  }

}
