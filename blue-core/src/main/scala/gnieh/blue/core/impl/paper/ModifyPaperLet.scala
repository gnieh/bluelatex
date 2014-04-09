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
import couch.Paper
import common._

import com.typesafe.config.Config

import tiscaf._

import gnieh.diffson._

import scala.io.Source

import scala.util.{
  Try,
  Success
}

import gnieh.sohva.control.CouchClient

/** Handle JSON Patches that add/remove/modify people involved in the given paper, tags,
 *  branches, ...
 *
 *  @author Lucas Satabin
 */
class ModifyPaperLet(paperId: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Unit] = role match {
    case Author =>
      // only authors may modify this list
      (talk.req.octets, talk.req.header("if-match")) match {
        case (Some(octets), knownRev @ Some(_)) =>
          val db = database(blue_papers)
          // the modification must be sent as a JSON Patch document
          // retrieve the paper object from the database
          db.getDocById[Paper](paperId) flatMap {
            case Some(paper) if paper._rev == knownRev =>
              talk.readJson[JsonPatch] match {
                case Some(patch) =>
                  // the revision matches, we can apply the patch
                  val paper1 = patch(paper).withRev(knownRev)
                  // and save the new paper data
                  db.saveDoc(paper1) map {
                    case Some(p) =>
                      // save successfully, return ok with the new ETag
                      // we are sure that the revision is not empty because it comes from the database
                      talk.writeJson(true, p._rev.get)
                    case None =>
                      // something went wrong
                      talk
                        .setStatus(HStatus.InternalServerError)
                        .writeJson(ErrorResponse("unknown_error", "An unknown error occured"))
                  }
                case None =>
                  // nothing to do
                  Success(
                    talk
                      .setStatus(HStatus.NotModified)
                      .writeJson(ErrorResponse("nothing_to_do", "No changes sent")))
              }
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
          .writeJson(ErrorResponse("no_sufficient_rights", "Only authors may modify the list of involved people")))
  }

}

