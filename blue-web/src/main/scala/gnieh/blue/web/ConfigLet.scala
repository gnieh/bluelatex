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

import com.typesafe.config.Config

import http.BlueLet

import scala.util.Try

/** Returns the base configuration data needed by the client.
 *
 *  @author Lucas Satabin
 */
class ConfigLet(context: BundleContext, config: Config) extends HSimpleLet {

  import BlueLet._

  def act(talk: HTalk): Unit = {
    val recaptcha = Try(config.getString("recaptcha.public-key")).toOption
    val compilationType = Try(config.getString("compiler.compilation-type")).toOption
    talk.writeJson(
      AppConfig(
        config.getString("blue.api.path-prefix"),
        recaptcha,
        compilationType)
    )
   }

}
