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
package mobwrite

import org.osgi.framework._

import common.ConfigurationLoader

/** Registers the mobwrite service that delegates synchronization
 *  to a standalone process
 *
 *  @author Lucas Satabin
 */
class MobwriteActivator extends BundleActivator {

  import common.OsgiUtils._

  def start(context: BundleContext): Unit = {
    for(loader <- context.get[ConfigurationLoader]) {
      // instantiate the mobwrite server as synchronization server
      val server = new MobwriteServer(loader.load(context.getBundle.getSymbolicName, getClass.getClassLoader))
      // register this as the synchronization server
      context.registerService(classOf[SynchroServer], server, null)
    }
  }

  def stop(context: BundleContext): Unit = {
  }

}
