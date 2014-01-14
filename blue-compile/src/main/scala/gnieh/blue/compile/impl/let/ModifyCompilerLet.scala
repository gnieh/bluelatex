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

import tiscaf._

import com.typesafe.config.Config

import scala.util.{
  Try,
  Success,
  Failure
}


import gnieh.diffson.JsonPatch

/** Handle JSON Patches that modify the compiler data
 *
 *  @author Lucas Satabin
 */
class ModifyCompilerLet(paperId: String, config: Config, logger: Logger) extends SyncRoleLet(paperId, config, logger) {

  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Any] = role match {
    case Author =>
      (talk.req.octets, talk.req.header("if-match")) match {
        case (Some(octets), Some(knownRev)) =>
          val db = database(blue_papers)
          // the modification must be sent as a JSON Patch document
          // retrieve the settings object from the database
          db.getDocById[CompilerSettings](s"$paperId:compiler") flatMap {
            case Some(settings) if settings._rev == knownRev =>
              talk.readJson[JsonPatch] match {
                case Some(patch) =>
                  // the revision matches, we can apply the patch
                  val settings1 = patch(settings)
                  // and save the new compiler data
                  db.saveDoc(settings1) map {
                    case Some(s) =>
                      // save successfully, return ok with the new ETag
                      // we are sure that the revision is not empty because it comes from the database
                      talk.writeJson(true, s._rev.get)
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

            case Some(settings) =>
              // old revision sent
              Success(talk.writeJson(ErrorResponse("conflict", "Old settings revision provided"))
                .setStatus(HStatus.Conflict))

            case None =>
              // unknown paper
              Success(talk.writeJson(ErrorResponse("nothing_to_do", s"Unknown compiler for paper $paperId"))
                .setStatus(HStatus.NotFound))
          }

        case (None, _) =>
          // nothing to do
          Success(talk.writeJson(ErrorResponse("nothing_to_do", "No changes sent"))
            .setStatus(HStatus.NotModified))

        case (_, None) =>
          // known revision was not sent, precondition failed
          Success(talk.writeJson(ErrorResponse("conflict", "Settings revision not provided"))
            .setStatus(HStatus.Conflict))

      }

    case _ =>
      Try(talk.writeJson(ErrorResponse("no_sufficient_rights", "Only authors may change compiler settings"))
        .setStatus(HStatus.Forbidden))
  }

}

