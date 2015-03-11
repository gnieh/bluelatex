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

import http._
import common._
import permission._

import tiscaf._

import com.typesafe.config.Config

import scala.util.{
  Try,
  Success,
  Failure
}

import gnieh.sohva.control.CouchClient

import gnieh.diffson.JsonPatch

import akka.actor.ActorRef

/** Handle JSON Patches that modify the compiler data
 *
 *  @author Lucas Satabin
 */
class ModifyCompilerLet(paperId: String, val couch: CouchClient, dispatcher: ActorRef, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: Option[UserInfo], role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Any] = permissions match {
    case Configure() =>
      (talk.req.octets, talk.req.header("if-match")) match {
        case (Some(octets), knownRev @ Some(_)) =>
          val manager = entityManager("blue_papers")
          // the modification must be sent as a JSON Patch document
          // retrieve the settings object from the database
          manager.getComponent[CompilerSettings](paperId) flatMap {
            case Some(settings) if settings._rev == knownRev =>
              talk.readJson[JsonPatch] match {
                case Some(patch) =>
                  // the revision matches, we can apply the patch
                  val settings1 = patch(settings).withRev(knownRev)
                  // and save the new compiler data
                  for(s <- manager.saveComponent(paperId, settings1))
                    yield {
                      dispatcher ! Forward(paperId, settings1)
                      // save successfully, return ok with the new ETag
                      // we are sure that the revision is not empty because it comes from the database
                      talk.writeJson(true, s._rev.get)
                    }
                case None =>
                  // nothing to do
                  Success(
                    talk
                      .setStatus(HStatus.NotModified)
                      .writeJson(ErrorResponse("nothing_to_do", "No changes sent")))
              }

            case Some(settings) =>
              // old revision sent
              Success(
                talk
                  .setStatus(HStatus.Conflict)
                  .writeJson(ErrorResponse("conflict", "Old settings revision provided")))

            case None =>
              // unknown compiler
              Success(
                talk
                  .setStatus(HStatus.NotFound)
                  .writeJson(ErrorResponse("nothing_to_do", s"Unknown compiler for paper $paperId")))
          }

        case (None, _) =>
          Success(
            talk
              .setStatus(HStatus.NotModified)
              .writeJson(ErrorResponse("nothing_to_do", "No changes sent")))

        case (_, None) =>
          // known revision was not sent, precondition failed
          Success(
            talk
              .setStatus(HStatus.Conflict)
              .writeJson(ErrorResponse("conflict", "Settings revision not provided")))

      }

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to change compiler settings")))
  }

}
