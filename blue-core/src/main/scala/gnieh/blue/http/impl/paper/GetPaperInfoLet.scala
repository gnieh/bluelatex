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

import com.typesafe.config.Config

import tiscaf._

import gnieh.sohva.UserInfo

import scala.io.Source

/** Returns the paper data
 *
 *  @author Lucas Satabin
 */
class GetPaperInfoLet(paperid: String, config: Config) extends AuthenticatedLet(config) {

  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Unit = {
    // only authenticated users may see other people information
    database(blue_papers).getDocById[Paper](paperid) match {
      // we are sure that the paper has a revision because it comes from the database
      case Some(paper) => talk.writeJson(paper, paper._rev.get)
      case None       => talk.writeJson(ErrorResponse("not_found", s"Paper $paperid not found")).setStatus(HStatus.NotFound)
    }
  }

}
