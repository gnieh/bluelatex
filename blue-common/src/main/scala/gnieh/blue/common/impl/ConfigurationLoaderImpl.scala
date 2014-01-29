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
package common
package impl

import java.net.{
  URLClassLoader,
  URL
}
import java.io.File

import com.typesafe.config._

class ConfigurationLoaderImpl(base: File) extends ConfigurationLoader {

  val globalConf = ConfigFactory.load(new URLClassLoader(optionalURL(base).toArray, getClass.getClassLoader))

  private def optionalURL(f: File) =
    if(f.exists)
      Some(f.toURI.toURL)
    else
      None

  def load(bundleName: String, parent: ClassLoader): Config = {
    // load configuration from the base configuration directory, the bundle specific directory and the bundle class loader
    val directories = optionalURL(new File(base, bundleName)).toArray
    val cl = new URLClassLoader(directories, parent)
    ConfigFactory.load(cl).withFallback(globalConf)
  }

}
