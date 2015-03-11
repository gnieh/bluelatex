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
package core
package impl
package paper

import http._
import couch.PaperPhase
import common._
import permission._

import com.typesafe.config.Config

import tiscaf._

import gnieh.diffson._

import scala.io.Source

import scala.util.{
  Try,
  Success
}

import gnieh.sohva.control.CouchClient

/** Handle JSON Patches that add/remove/modify permissions for the given paper
 *
 *  @author Lucas Satabin
 */
class ModifyPermissionsLet(paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncPermissionLet(paperId, config, logger) {

  def permissionAct(user: Option[UserInfo], role: Role, permissions: List[Permission])(implicit talk: HTalk): Try[Unit] = permissions match {
    case Configure() =>
      // only authors may modify this list
      (talk.req.octets, talk.req.header("if-match")) match {
        case (Some(octets), knownRev @ Some(_)) =>
          val manager = entityManager("blue_papers")
          // the modification must be sent as a JSON Patch document
          // retrieve the paper object from the database
          manager.getComponent[PaperPhase](paperId) flatMap {
            case Some(phase @ PaperPhase(_, _, permissions, _)) if phase._rev == knownRev =>
              talk.readJson[JsonPatch] match {
                case Some(patch) =>
                  // the revision matches, we can apply the patch
                  val permissions1 = patch(permissions)
                  val permissions2 =
                    phase.copy(permissions = permissions1).withRev(knownRev)
                  // and save the new paper data
                  for(r <- manager.saveComponent(paperId, permissions2))
                    // save successfully, return ok with the new ETag
                    // we are sure that the revision is not empty because it comes from the database
                    yield talk.writeJson(true, r._rev.get)
                case None =>
                  // nothing to do
                  Success(
                    talk
                      .setStatus(HStatus.NotModified)
                      .writeJson(ErrorResponse("nothing_to_do", "No changes sent")))
              }
            case Some(_) =>
              // nothing to do
              Success(
                talk
                  .setStatus(HStatus.Conflict)
                  .writeJson(ErrorResponse("conflict", "Old role revision provided")))

            case None =>
              // unknown paper
              Success(
                talk
                  .setStatus(HStatus.NotFound)
                  .writeJson(ErrorResponse("nothing_to_do", s"Unknown paper $paperId")))

          }

        case (None, _) =>
          // nothing to do
          Success(
            talk
              .setStatus(HStatus.NotModified)
              .writeJson(ErrorResponse("nothing_to_do", "No changes sent")))

        case (_, None) =>
          // known revision was not sent, precondition failed
          Success(
            talk
              .setStatus(HStatus.Conflict)
              .writeJson(ErrorResponse("conflict", "Paper revision not provided")))
      }
    case _ =>
      Success(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "You have no permission to modify the permissions")))
  }

}

