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

import couch.impl._
import http.impl._

import org.osgi.framework._
import org.osgi.service.log.LogService

import akka.actor.ActorSystem
import akka.osgi.ActorSystemActivator
import akka.util._

import scala.concurrent.duration._

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator

import java.io.File
import java.util.concurrent.TimeUnit

import com.typesafe.config._

import gnieh.sohva.control.CouchClient
import gnieh.sohva.control.entities.EntityManager
import gnieh.sohva.JsonSerializer

/** Register the configuration loader service that is used by everybody
 *
 *  @author Lucas Satabin
 */
class BlueCommonActivator extends ActorSystemActivator {

  import FileUtils._
  import OsgiUtils._

  private var dbManager: Option[DbManager] = None
  private var couch: Option[CouchClient] = None
  private var templates: Option[Templates] = None
  private var server: Option[BlueServer] = None

  def configure(context: BundleContext, system: ActorSystem): Unit = {
    val configBase = new File(context.getProperty("blue.configuration.base"))
    // the bundle configuration loader server
    val loader = new ConfigurationLoaderImpl(context.getBundle.getSymbolicName, configBase)
    // register it
    context.registerService(classOf[ConfigurationLoader], loader, null)

    // configure the logging framework
    val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    try {
      val configurator = new JoranConfigurator
      configurator.setContext(loggerContext)
      configurator.doConfigure(configBase / "logback.xml")
    } catch {
      case e: Exception =>
        // TODO what to do?
        e.printStackTrace
    }
    context.registerService(classOf[LogService].getName, new LogServiceFactory, null)

    for(logger <- context.get[Logger]) try {
      val config = loader.load(context.getBundle)

      // register the couch client service
      val client = couch(system, config)
      context.registerService(classOf[CouchClient], client, null)

      // create the database, etc...
      val couchConfig = new CouchConfiguration(config)
      dbManager = Some(new DbManager(client, couchConfig, logger))
      dbManager.foreach(_.start())
      // force the creation of design documents for entities if they don't exist
      couchConfig.asAdmin(client) { session =>
        new EntityManager(session.database(couchConfig.database("blue_papers"))).entities("")
        new EntityManager(session.database(couchConfig.database("blue_users"))).entities("")
      }

      val configuration = new BlueConfiguration(config)

      // register the mail agent client
      val mailAgent = new MailAgentImpl(client, configuration, logger)
      context.registerService(classOf[MailAgent], mailAgent, null)

      // register the recaptcha service
      val recaptcha = new ReCaptchaUtilImpl(configuration)
      context.registerService(classOf[ReCaptcha], recaptcha, null)

      // create and start the http server
      server = Some(new BlueServer(context, system, config, logger))
      server.foreach(_.start)

      templates = Some(new TemplatesImpl(configuration))
      context.registerService(classOf[Templates], templates.get, null)

    } catch {
      case e: Exception =>
        logger.log(LogService.LOG_ERROR, s"Unable to start the core bundle", e)
        throw e
    }

    // register the actor system as service so that other bundle can use it
    registerService(context, system)

  }

  override def stop(context: BundleContext): Unit = {
    // stop the server
    server.foreach(_.stop)
    server = None
    // stop the template engine
    templates = None
    // stop the framework
    context.getBundle(0).stop
  }

  private def couch(system: ActorSystem, config: Config): CouchClient = {
    val hostname = config.getString("couch.hostname")
    val port = config.getInt("couch.port")
    val ssl = config.getBoolean("couch.ssl")
    val timeout = Timeout(config.getDuration("couch.timeout", TimeUnit.SECONDS).seconds)
    val c = new CouchClient(host = hostname, port = port, ssl = ssl, custom = List(BluePermissionSerializer))(system, timeout)
    couch = Some(c)
    c
  }

}

