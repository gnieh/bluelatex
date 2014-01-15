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


import tiscaf._

import scala.concurrent._
import duration._

import scala.util.{
  Try,
  Success,
  Failure
}

import akka.pattern.ask
import akka.util.Timeout

import com.typesafe.config.Config

class CompilerLet(paperId: String, dispatcher: ActorRef, config: Config, logger: Logger) extends AsyncRoleLet(paperId, config, logger) {

  // TODO make it configurable with connection keep-alive timeout
  implicit val timeout = Timeout(20.seconds)

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Future[Any] =
    for(result <- dispatcher.ask(Forward(paperId, Register)).mapTo[Try[Boolean]])
      yield result match {
        case Success(ok) =>
          talk.writeJson(ok)
        case Failure(t) =>
          logError(s"Could not compile paper $paperId", t)
          talk
            .setStatus(HStatus.InternalServerError)
            .writeJson(ErrorResponse("unable_to_compile", s"Something went wrong when compiling paper $paperId"))
      }

}

