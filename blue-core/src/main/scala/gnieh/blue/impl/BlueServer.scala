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
package impl

import tiscaf._

import org.osgi.framework.BundleContext

class BlueServer(context: BundleContext, configuration: BlueConfiguration) extends HServer with Logging {

  protected val ports = Set(configuration.port)
  protected val apps = List(restApp)

  val restApp = new RestServiceTracker(context)

  override protected def maxPostDataLength = 100000000

  override protected def onError(t: Throwable) = t match {
    case e: Exception =>
      logger.error("Oooops something wrong happened…", e)
    case e: Error =>
      logger.error("Aaaaaaaargh something really wrong happened… Abort! Abort!", e)
      stop
      sys.exit(1)
  }

  override protected def onStart {
    // start the application tracker
    restApp.open
  }

  override protected def onStop {
    // stop the application tracker
    restApp.close
  }

  logger.info("blue server started")

}
