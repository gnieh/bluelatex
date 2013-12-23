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
package config.impl

import org.osgi.framework.{
  Bundle,
  Version,
  ServiceReference
}
import org.osgi.service.log.LogService

import org.slf4j.LoggerFactory

/** Instantiate one logger per bundle
 *
 *  @author Lucas Satabin
 */
class LogServiceImpl(bundle: Bundle) extends LogService {

  import LogService._

  val loggerName = {
    // the logger name is made out of the sybolic name and version
    // this pair is unique in the OSGi container
    val bname = bundle.getSymbolicName
    val bversion = Option(bundle.getVersion).getOrElse(Version.emptyVersion)
    s"$bname:bversion"
  }

  val underlying =
    LoggerFactory.getLogger(loggerName)

  @inline
  def log(ref: ServiceReference[_], level: Int, msg: String, exn: Throwable): Unit =
    doLog(Some(ref), level, msg, Some(exn))

  @inline
  def log(ref: ServiceReference[_], level: Int, msg: String): Unit =
    doLog(Some(ref), level, msg, None)

  @inline
  def log(level: Int, msg: String, exn: Throwable): Unit =
    doLog(None, level, msg, Some(exn))

  @inline
  def log(level: Int, msg: String): Unit =
    doLog(None, level, msg, None)

  def doLog(ref: Option[ServiceReference[_]], level: Int, msg: String, exn: Option[Throwable]): Unit = {
    val message = format(ref, msg)
    (level, exn) match {
      case (LOG_DEBUG, Some(exn)) =>
        underlying.debug(message, exn)
      case (LOG_DEBUG, None) =>
        underlying.debug(message)
      case (LOG_INFO, Some(exn)) =>
        underlying.info(message, exn)
      case (LOG_INFO, None) =>
        underlying.info(message)
      case (LOG_WARNING, Some(exn)) =>
        underlying.warn(message, exn)
      case (LOG_WARNING, None) =>
        underlying.warn(message)
      case (LOG_ERROR, Some(exn)) =>
        underlying.error(message, exn)
      case (LOG_ERROR, None) =>
        underlying.error(message)
      case (level, Some(exn)) =>
        underlying.info(message, exn)
      case (level, None) =>
        underlying.info(message)
    }
  }

  def format(ref: Option[ServiceReference[_]], msg: String): String =
    ref match {
      case Some(ref) =>
        s"[service: $ref] ${msg.trim}"
      case None =>
        msg.trim
    }

}

