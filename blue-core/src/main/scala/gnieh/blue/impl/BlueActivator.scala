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

import http.RestApi
import http.impl.{
  BlueServer,
  CoreApi
}
import couch.DbManager

import gnieh.sohva.sync._


/** The `BlueActivator` starts the \BlueLaTeX core system:
 *   - the configuration loader
 *   - the actor system
 *
 *  @author Lucas Satabin
 */
class BlueActivator extends ActorSystemActivator {

  private var templates: Option[Templates] = None
  private var couch: Option[CouchClient] = None
  private var server: Option[BlueServer] = None
  private var dbManager: Option[DbManager] = None

  def configure(context: BundleContext, system: ActorSystem): Unit = {

    // the bundle configuration loader server
    val loader = new ConfigurationLoaderImpl(new File(context.getProperty("blue.configuration.base")))
    // register it
    context.registerService(classOf[ConfigurationLoader], loader, null)
    // load the \BlueLaTeX common configuration
    val config = loader.load(context.getBundle.getSymbolicName, getClass.getClassLoader)
    val configuration = new BlueConfiguration(config)

    // create and start the http server
    server = Some(new BlueServer(context, config))
    server.map(_.start)

    // register the template engine
    // set the context classloader to the bundle classloader, because
    // scalate uses this classloader to determine whether we are in an OSGi context
    val previousCl = Thread.currentThread.getContextClassLoader
    Thread.currentThread.setContextClassLoader(getClass.getClassLoader)
    templates = Some(new TemplatesImpl(configuration))
    // initialize the template compiler on start
    templates.foreach(_.engine.compiler)
    Thread.currentThread.setContextClassLoader(previousCl)
    context.registerService(classOf[Templates], templates.get, null)

    // register the mail agent client
    val mailAgent = new MailAgentImpl(configuration)
    context.registerService(classOf[MailAgent], mailAgent, null)

    // register the recaptcha service
    val recaptcha = new ReCaptchaUtilImpl(configuration)
    context.registerService(classOf[ReCaptcha], recaptcha, null)

    // register the couch client service
    context.registerService(classOf[CouchClient], couch(config), null)

    // create the database, etc...
    dbManager = Some(new DbManager(new CouchConfiguration(config)))
    dbManager.foreach(_.start())

    // register the actor system as service so that other bundle can use it
    registerService(context, system)

    // register the core Rest API
    context.registerService(classOf[RestApi], new CoreApi(config, templates.get, mailAgent, recaptcha), null)

  }

  private def couch(config: Config): CouchClient = {
    val hostname = config.getString("couch.hostname")
    val port = config.getInt("couch.port")
    val ssl = config.getBoolean("couch.ssl")
    val c = new CouchClient(host = hostname, port = port, ssl = ssl)
    couch = Some(c)
    c
  }

  override def stop(context: BundleContext): Unit = {

    // stop the server
    server.foreach(_.stop)
    // stop the actor system, etc...
    super.stop(context)
    // stop the couch client
    couch.foreach(_.shutdown)
    // stop the template engine
    templates.foreach(_.engine.compiler.shutdown())
    // stop the framework
    context.getBundle(0).stop

  }

}
