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
package core
package impl
package session

import com.typesafe.config.Config

import http._
import common._

import tiscaf._

import gnieh.sohva.control.CouchClient

import scala.util.{
  Try,
  Success
}

/** Get the session data if the request is authenticated.
 *
 *  @author Lucas Satabin
 */
class GetSessionDataLet(val couch: CouchClient, config: Config, logger: Logger) extends SyncBlueLet(config, logger) with SyncAuthenticatedLet {

  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Try[Unit] =
    Success(talk.writeJson(user))

}

