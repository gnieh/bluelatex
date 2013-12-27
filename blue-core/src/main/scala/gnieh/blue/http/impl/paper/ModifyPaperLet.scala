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
package http
package impl
package paper

import couch.Paper
import common._

import com.typesafe.config.Config

import tiscaf._

import gnieh.diffson._

import gnieh.sohva.UserInfo

import scala.io.Source

import scala.util.{
  Try,
  Success
}

/** Handle JSON Patches that add/remove/modify people involved in the given paper, tags,
 *  branches, ...
 *
 *  @author Lucas Satabin
 */
class ModifyPaperLet(paperId: String, config: Config, logger: Logger) extends RoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Unit] = role match {
    case Author =>
      // only authors may modify this list
      (talk.req.octets, talk.req.header("if-match")) match {
        case (Some(octets), Some(knownRev)) =>
          val db = database(blue_papers)
          // the modification must be sent as a JSON Patch document
          // retrieve the paper object from the database
          db.getDocById[Paper](paperId) flatMap {
            case Some(paper) if paper._rev == knownRev =>
              talk.readJson[JsonPatch] match {
                case Some(patch) =>
                  // the revision matches, we can apply the patch
                  val paper1 = patch(paper)
                  // and save the new paper data
                  db.saveDoc(paper1) map {
                    case Some(p) =>
                      // save successfully, return ok with the new ETag
                      // we are sure that the revision is not empty because it comes from the database
                      talk.writeJson(true, p._rev.get)
                    case None =>
                      // something went wrong
                      talk.writeJson(ErrorResponse("unknown_error", "An unknown error occured"))
                        .setStatus(HStatus.InternalServerError)
                  }
                case None =>
                  // nothing to do
                  Success(talk.writeJson(ErrorResponse("nothing_to_do", "No changes sent"))
                    .setStatus(HStatus.NotModified))
              }
            case None =>
              // unknown paper
              Success(talk.writeJson(ErrorResponse("nothing_to_do", s"Unknown paper $paperId"))
                .setStatus(HStatus.NotFound))
          }

        case (None, _) =>
          // nothing to do
          Success(talk.writeJson(ErrorResponse("nothing_to_do", "No changes sent"))
            .setStatus(HStatus.NotModified))

        case (_, None) =>
          // known revision was not sent, precondition failed
          Success(talk.writeJson(ErrorResponse("conflict", "Paper revision not provided"))
            .setStatus(HStatus.Conflict))
      }
    case _ =>
      Success(talk.writeJson(ErrorResponse("no_sufficient_rights", "Only authors may modify the list of involved people"))
        .setStatus(HStatus.Forbidden))
  }

}
