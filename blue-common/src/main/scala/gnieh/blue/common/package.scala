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

import com.typesafe.config.{
  Config,
  ConfigValue
}

import scala.collection.JavaConverters._
import scala.util.Try

package object common {

  type Logger = org.osgi.service.log.LogService

  type UserInfo = gnieh.sohva.UserInfo

  implicit class PermissionsConfig(val config: Config) extends AnyVal {

    def getBuiltInPermissions: Map[String, Map[String, Set[String]]] =
        if(config.hasPath("blue.permissions")) {
          val permissions = config.getObject("blue.permissions")
          Try {
            for {
              (name, _) <- permissions.asScala
            } yield (name, toMap(permissions.toConfig().getConfig(name)))
          } map (_.toMap) getOrElse(Map())
        } else {
          Map()
        }

  }

  def toMap(value: Config): Map[String, Set[String]] =
    Map(
      "author" -> (if(value.hasPath("author")) value.getStringList("author").asScala.toSet else Set()),
      "reviewer" -> (if(value.hasPath("reviewer")) value.getStringList("reviewer").asScala.toSet else Set()),
      "guest" -> (if(value.hasPath("guest")) value.getStringList("guest").asScala.toSet else Set()),
      "other" -> (if(value.hasPath("other")) value.getStringList("other").asScala.toSet else Set()),
      "anonymous" -> (if(value.hasPath("anonymous")) value.getStringList("anonymous").asScala.toSet else Set()))

}

