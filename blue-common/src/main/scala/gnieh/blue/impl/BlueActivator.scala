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

import java.io.File

import org.osgi.framework._

import akka.actor.ActorSystem
import akka.osgi.ActorSystemActivator

import com.typesafe.config._

import util._

import gnieh.sohva.sync._

/** The `BlueActivator` starts the \BlueLaTeX core system:
 *   - the configuration loader
 *   - the actor system
 *
 *  @author Lucas Satabin
 */
class BlueActivator extends ActorSystemActivator {

  private var _templates: Templates = _
  private var _couch: CouchClient = _

  def configure(context: BundleContext, system: ActorSystem): Unit = {

    // the bundle configuration loader server
    val loader = new ConfigurationLoaderImpl(new File(context.getProperty("blue.configuration.base")))
    // register it
    context.registerService(classOf[ConfigurationLoader], loader, null)
    // load the \BlueLaTeX common configuration
    val config = loader.load(context.getBundle.getSymbolicName)
    val configuration = new BlueConfiguration(config)
    // register the template engine
    _templates = new TemplatesImpl(configuration)
    context.registerService(classOf[Templates], _templates, null)
    // register the recaptcha service
    context.registerService(classOf[ReCaptcha], new ReCaptchaUtilImpl(configuration), null)
    // register the couch client service
    context.registerService(classOf[CouchClient], couch(config), null)

    // register the actor system as service so that other bundle can use it
    registerService(context, system)

  }

  private def couch(config: Config) = {
    val hostname = config.getString("couch.hostname")
    val port = config.getInt("couch.port")
    val ssl = config.getBoolean("couch.ssl")
    _couch = new CouchClient(host = hostname, port = port, ssl = ssl)
    _couch
  }

  override def stop(context: BundleContext): Unit = {

    // stop the actor system, etc...
    super.stop(context)
    // stop the couch client
    _couch.shutdown
    // stop the template engine
    _templates.engine.compiler.shutdown()
    // stop the framework
    context.getBundle(0).stop

  }

}
