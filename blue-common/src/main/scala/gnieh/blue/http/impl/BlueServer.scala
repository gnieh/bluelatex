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

import akka.actor.ActorSystem

import org.osgi.framework.BundleContext
import org.osgi.util.tracker.ServiceTracker

import com.typesafe.config._

import scala.collection.mutable.{
  Map,
  ListBuffer
}

import common._

class BlueServer(context: BundleContext, system: ActorSystem, configuration: Config, val logger: Logger) extends HServer with Logging {

  protected val ports =
    Set(configuration.getInt("http.port"))

  private val extApp =
    new ExtensibleApp(configuration, system)

  private var allApps =
    Vector[HApp](extApp)

  protected def apps =
    allApps

  private val restTracker =
    new RestServiceTracker(context, extApp)

  import OsgiUtils._

  private val appTracker =
    context.trackAll[HApp] {
      case ServiceAdded(app)   => allApps = allApps :+ app
      case ServiceRemoved(app) => allApps = allApps.filter(_ ne app)
    }

  override protected def maxPostDataLength =
    100000000

  override protected def onStart: Unit = {
    // start the application tracker
    restTracker.open()
  }

  override protected def onStop: Unit = {
    // stop the application tracker
    restTracker.close()
    appTracker.close()
  }

  logInfo("blue server started")

}

