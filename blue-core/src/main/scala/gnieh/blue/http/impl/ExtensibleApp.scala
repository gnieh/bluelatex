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
package http
package impl

import tiscaf.{
  HApp,
  HLet,
  HReqData,
  HReqType,
  HTracking
}

import scala.collection.{ mutable => mu }

import com.typesafe.config.Config

import gnieh.sohva.control.CouchSession

/** The rest interface may be extended by \BlueLaTeX modules.
 *  Theses module simply need to register services implementing this trait
 *  to make the new interface available.
 *
 *  '''Note''': make sure that the interface in your module does not collide
 *  with another existing and already registered module. In such a case
 *
 *  @author Lucas Satabin
 */
class ExtensibleApp(config: Config) extends HApp {

  private[impl] val apps = mu.Map.empty[Long, RestApi]
  private def gets =
    (for((_, app) <- apps)
      yield app.gets).toList.flatten
  private def posts =
    (for((_, app) <- apps)
      yield app.posts).toList.flatten
  private def puts =
    (for((_, app) <- apps)
      yield app.puts).toList.flatten
  private def patches =
    (for((_, app) <- apps)
      yield app.patches).toList.flatten
  private def deletes =
    (for((_, app) <- apps)
      yield app.deletes).toList.flatten

  // session stuffs
  override def tracking = HTracking.Cookie
  override def sessionTimeoutMinutes = config.getInt("blue.session-timeout")
  override def cookieKey = "BLUE_SESSIONID"

  override def onSessionInvalidate(sid: String, data: Map[Any, Any]) {
    // logout the couchdb session if any
    for(session <- data.get(SessionKeys.Couch).collect { case s: CouchSession => s}) {
      session.logout
    }
  }

  final override def resolve(req: HReqData) = {
    val handlers = synchronized {
      req.method match {
        case HReqType.Get => gets
        case HReqType.PostData | HReqType.PostMulti | HReqType.PostOctets =>
          posts
        case HReqType.Delete => deletes
        case HReqType.Put    => puts
        case HReqType.Patch  => patches
        case _               => throw new RuntimeException("Unknown request type")
      }
    }

    // find the first
    handlers.find(_.isDefinedAt(req)) map (_(req))

  }

}
