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

import tiscaf._

import org.osgi.framework._
import org.osgi.util.tracker.ServiceTracker

import scala.collection.mutable.Map

/** Tracks the `RestHandler` services that are (un)registered to dynamically extend
 *  the server interface
 *
 *  @author Lucas Satabin
 */
class RestServiceTracker(context: BundleContext)
  extends ServiceTracker[RestHandler, Long](context, classOf[RestHandler], null)
  with HApp {

  private val gets = Map.empty[Long, PartialFunction[(List[String], HReqData), HLet]]
  private val posts = Map.empty[Long, PartialFunction[(List[String], HReqData), HLet]]
  private val deletes = Map.empty[Long, PartialFunction[(List[String], HReqData), HLet]]

  private def getHandler = gets.values
  private def postHandler = gets.values
  private def deleteHandler = gets.values

  final override def resolve(req: HReqData) = {
    val handlers = synchronized {
      req.method match {
        case HReqType.Get => getHandler
        case HReqType.PostData | HReqType.PostMulti | HReqType.PostOctets =>
          postHandler
        case HReqType.Delete => deleteHandler
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

  override def addingService(ref: ServiceReference[RestHandler]): Long = {
    // get the service instance
    val service = context.getService(ref)
    // get the servive identifier
    val id = ref.getProperty(Constants.SERVICE_ID).asInstanceOf[Long]
    synchronized {
      // register the rest interface with the generated identifier
      if(service.verb == HReqType.Get)
        gets(id) = service.handler
      if(service.verb == HReqType.PostData)
        posts(id) = service.handler
      if(service.verb == HReqType.Delete)
        deletes(id) = service.handler
    }
    id
  }

  override def removedService(ref: ServiceReference[RestHandler], id: Long): Unit = synchronized {
    // unregister the handlers for the given identifier
    gets -= id
    posts -= id
    deletes -= id
  }

}
