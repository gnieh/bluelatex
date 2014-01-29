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
package impl

import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Await

import scala.util.Try

import java.io.File

/** A LaTeX compiler that calls a compiler installed on the host system.
 *
 *  @author Lucas Satabin
 */
abstract class SystemCompiler(system: ActorSystem) extends Compiler {

  private val systemCommand = system.actorSelection("/user/system-commands")

  protected def exec(command: String, workingDir: File, env: List[(String, String)] = List())(implicit timeout: Timeout) =
    Try {
      Await.result(
        systemCommand ? SystemCommand(command, workingDir, env, timeout) mapTo manifest[Int],
        timeout.duration
      ) == 0
    }

}

