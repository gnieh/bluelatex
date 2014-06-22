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
package scenario

import couch.{
  Paper => BluePaper,
  PaperRole,
  UsersGroups
}

import java.util.Date

import org.scalatest._

import gnieh.sohva.CouchUser

/** Mix in this trait to add some papers that are already present in your database
 *  before executing each scenario
 *
 *  @author Lucas Satabin
 */
trait SomePapers extends BeforeAndAfterEach {
  this: BlueScenario =>

  val paper1 = Paper("paper1", "Some Paper", Set("glambert"), Set(), new Date)
  val paper2 = Paper("paper2", "Some Other Paper", Set("toto"), Set("glambert"), new Date)

  val predefinedPapers: List[Paper]

  override def beforeEach(): Unit = try {
    super.beforeEach()
  } finally {
    // save the papers into the database
    try {
      for(paper <- predefinedPapers) {
        paperManager.create(paper._id, None)
        paperManager.saveComponent(paper._id, BluePaper(s"${paper._id}:core", paper.name, paper.creation_date))
        paperManager.saveComponent(paper._id,
          PaperRole(
            s"${paper._id}:roles",
            UsersGroups(paper.authors, Set()),
            UsersGroups(paper.reviewers, Set()),
            UsersGroups(Set(), Set())))
      }
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

  override def afterEach(): Unit = try {
    super.afterEach()
  } finally {
    // save the papers
    for(Paper(id, _, _, _, _) <- predefinedPapers)
      paperManager.deleteEntity(id)
  }

}

