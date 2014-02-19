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

import http._
import common._
import let._

import com.typesafe.config.Config

/** The synchronization service API exposes an interface for clients
 *  to synchronize their paper
 *
 *  @author Lucas Satabin
 */
class SyncApi(config: Config, synchroServer: SynchroServer, logger: Logger) extends RestApi {

  POST {
    case p"papers/$paperid/q" =>
      new QLet(paperid, synchroServer, config, logger)
    case p"papers/$paperid/sync" =>
      new SynchronizePaperLet(paperid, synchroServer, config, logger)
  }

}

