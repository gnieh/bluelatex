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

import org.osgi.framework.BundleContext

import tiscaf.{
  HApp,
  HLet,
  HReqData,
  HReqType
}

import scala.collection.mutable.ListBuffer

/** The rest interface may be extended by \BlueLaTeX modules.
 *  Theses module simply need to register services implementing this trait
 *  to make the new interface available.
 *
 *  '''Note''': make sure that the interface in your module does not collide
 *  with another existing and already registered module. In such a case
 *
 *  @author Lucas Satabin
 */
trait RestApplication extends HApp {

  private val posts = ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet]]
  private val gets = ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet]]
  private val deletes = ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet]]

  final override def resolve(req: HReqData) = {
    val handlers = synchronized {
      req.method match {
        case HReqType.Get => gets
        case HReqType.PostData | HReqType.PostMulti | HReqType.PostOctets =>
          posts
        case HReqType.Delete => deletes
        case _               => throw new RuntimeException("Unknown request type")
      }
    }

    val splitted = req.uriPath.split("/").toList

    // find the first
    handlers.find(_.isDefinedAt(splitted, req)) match {
      case Some(handler) =>
        Some(handler(splitted, req))
      case _ => None
    }

  }

  def POST(handler: PartialFunction[(List[String], HReqData), HLet]) {
    posts += handler
  }

  def GET(handler: PartialFunction[(List[String], HReqData), HLet]) {
    gets += handler
  }

  def DELETE(handler: PartialFunction[(List[String], HReqData), HLet]) {
    deletes += handler
  }

}
