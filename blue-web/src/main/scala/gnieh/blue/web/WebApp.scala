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

import org.osgi.framework.BundleContext

import com.typesafe.config.Config

/** This web application serves static web client
 *
 *  @author Lucas Satabin
 */
class WebApp(context: BundleContext, config: Config) extends HApp {

  // this is required by the resource let used to serve static content
  override val buffered = true

  private val Prefix = "/*(.*)/*".r

  private val prefix = config.getString("blue.client.path-prefix") match {
    case Prefix(prefix) => prefix
    case _              => ""
  }

  private val configLet = new ConfigLet(context, config)
  private val webLet = new WebLet(context, prefix)

  private val configPath =
    s"${if(prefix.nonEmpty) prefix + "/" else ""}configuration"

  def resolve(req: HReqData) =
    if(req.uriPath == configPath)
      Some(configLet)
    else
      Some(webLet)

}

