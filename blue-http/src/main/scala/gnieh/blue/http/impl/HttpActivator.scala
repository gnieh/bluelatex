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

import org.osgi.framework.{
  BundleActivator,
  BundleContext
}
import org.osgi.util.tracker.ServiceTracker

class HttpActivator extends BundleActivator with OsgiUtils {

  private var server: BlueServer = _

  protected[this] var context: BundleContext = _

  def start(context: BundleContext): Unit = {
    this.context = context

    withService(classOf[ConfigurationLoader]) { loader =>
      // create and start the server
      server = new BlueServer(context, loader.load(context.getBundle.getSymbolicName))
      // and start it
      server.start
    }
  }

  override def stop(context: BundleContext): Unit = {
    // stop the server
    if(server != null)
      server.stop
  }
}
