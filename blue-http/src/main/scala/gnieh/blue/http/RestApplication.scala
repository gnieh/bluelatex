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

import tiscaf.{
  HLet,
  HReqData
}

import org.osgi.framework.BundleContext

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
trait RestApplication {

  private[http] val posts = ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet]]
  private[http] val gets = ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet]]
  private[http] val deletes = ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet]]

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
