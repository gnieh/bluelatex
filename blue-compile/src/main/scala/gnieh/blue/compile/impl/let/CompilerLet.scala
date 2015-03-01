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
package let

import akka.actor.ActorRef
import akka.pattern.ask

import http._
import common._
import permission._

import tiscaf._

import scala.concurrent._

import com.typesafe.config.Config

import gnieh.sohva.control.CouchClient

class CompilerLet(paperId: String, val couch: CouchClient, dispatcher: ActorRef, config: Config, logger: Logger) extends AsyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: UserInfo, role: Role, permissions: List[Permission])(implicit talk: HTalk): Future[Any] = permissions match {
    case Compile() =>
      val promise = Promise[CompilationStatus]()

      // register the client with the paper compiler
      dispatcher ! Forward(paperId, Register(user.name, promise))

      promise.future.map {
        case CompilationSucceeded | CompilationFailed(true) =>
          talk.writeJson(true)
        case CompilationFailed(false) =>
          talk
            .setStatus(HStatus.InternalServerError)
            .writeJson(ErrorResponse("unable_to_compile", "Compilation failed, more details in the compilation log file."))
        case CompilationAborted =>
          talk
            .setStatus(HStatus.ServiceUnavailable)
            .writeJson(ErrorResponse("unable_to_compile", s"No compilation task started"))
        case CompilationUnnecessary =>
          talk
            .setStatus(HStatus.NotModified)
            .writeJson(false)
      } recover {
        case e =>
          logError(s"Unable to compile paper $paperId", e)
          talk
            .setStatus(HStatus.InternalServerError)
            .writeJson(ErrorResponse("unable_to_compile", "Compilation failed, more details in the compilation log file."))
      }

    case _ =>
      Future.successful(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to compile the paper")))

  }

}

