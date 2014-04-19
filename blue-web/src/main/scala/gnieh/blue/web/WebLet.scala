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
package web

import tiscaf._
import let._

import org.osgi.framework.BundleContext

import java.io.File

/** Implements the tiscaf built-in HLet that makes it possible to server
 *  static resources from the class loader.
 *  This implementation overrides de `getResource` method to make it work
 *  in an OSGi container by looking for resrouces in the context of the bundle
 *  class loader
 *
 *  @author Lucas Satabin
 */
class WebLet(context: BundleContext) extends ResourceLet {

  protected def dirRoot = "webapp"

  override protected def uriRoot = "web"

  override protected def indexes = List("index.html")

  override protected def getResource(path: String): java.io.InputStream = {
    val url = context.getBundle.getResource(path)
    if (url == null)
      null
    else url.getProtocol match {
      case "jar" | "bundle"  =>
        val is = url.openStream
        try {
          is.available
          is
        } catch {
          case _: Exception => null
        }
      case "file" =>
        if(new File(url.toURI).isFile)
          url.openStream
        else
          null
      case _ =>
        null
    }
  }

}

