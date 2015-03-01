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
package compile

import common.{
  Logger,
  Logging
}

import akka.actor.{
  Actor,
  Status
}
import akka.util.Timeout

import scala.sys.process.{
  ProcessLogger,
  Process,
  ProcessBuilder
}

import scala.concurrent.{
  Future,
  Await
}

import java.io.File

/** An actor that executes a system command with the configured timeout
 *
 *  @author Lucas Satabin
 *
 */
class SystemCommandActor(val logger: Logger) extends Actor with Logging {

  private implicit def executionContext = context.system.dispatcher

  private object SystemProcessLogger extends ProcessLogger {
    def out(s: => String) = logInfo(s)
    def err(s: => String) = logError(s)
    def buffer[T](f: => T) = f
  }

  /* Execute the process with timeout.
   * Timeout management is purely done in scala and doesn't use any
   * system command for this.
   * It the process did not return within the given timeout, it is killed and
   * an exception is routed to the caller
   */
  private def exec(timeout: Timeout, process: ProcessBuilder) = {

    val proc = process.run(SystemProcessLogger)
    val res = Future {
      proc.exitValue()
    }
    try {
      Await.result(res, timeout.duration)
    } catch {
      case e: Exception =>
        // kill the process
        proc.destroy()
        // notify the caller
        sender ! Status.Failure(e)
    }
  }

  def receive = {
    case SystemCommand(command, workingDir, env, timeout) =>
      sender ! exec(timeout, Process(command, workingDir, env: _*))
  }

}

case class SystemCommand(command: String, workingDir: File, env: List[(String, String)], timeout: Timeout)
