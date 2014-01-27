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
package let

import common._
import http._

import tiscaf._

import com.typesafe.config.Config

import scala.util.Try

import gnieh.sohva.UserInfo

/** Legacy compatibility let, allowing mobwrite clients to synchornize papers
 *  with the new implementation.
 *  This let converts from the legacy protocol to the new one before sending the commands to the
 *  synchronization server, and convert it back to the old protocol before sending it back
 *  to the client.
 *
 *  It assumes that the client respects following conventions:
 *   - user name (sent via `F:`) must be of the form `<paper_id>/<file>`
 *
 *  @author Lucas Satabin
 */
class QLet(paperId: String, synchroServer: SynchroServer, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Any] = role match {
    case Author =>
      ???
    case _ =>
      ???
  }

}

