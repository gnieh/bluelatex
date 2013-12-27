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

import org.osgi.service.log.LogService

/** Mixin this trait in any class you want to have support for logging
 *
 *  @author Lucas Satabin
 *
 */
trait Logging {

  import LogService._

  val logger: Logger

  @inline
  def logDebug(msg: String): Unit =
    logger.log(LOG_DEBUG, msg)

  @inline
  def logInfo(msg: String): Unit =
    logger.log(LOG_INFO, msg)

  @inline
  def logWarn(msg: String): Unit =
    logger.log(LOG_WARNING, msg)

  @inline
  def logError(msg: String, exn: Exception): Unit =
    logger.log(LOG_ERROR, msg, exn)

  @inline
  def logError(msg: String): Unit =
    logger.log(LOG_ERROR, msg)

}
