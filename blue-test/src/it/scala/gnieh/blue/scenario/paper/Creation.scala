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
package paper

import org.scalatest._

/** Scenarios to test the paper creation feature:
 *   - creation of a new paper
 *   - invalid paper creation requests
 *
 *  @author Lucas Satabin
 */
class PaperCreationSpec extends BlueScenario with SomeUsers {

  val predefinedPeople = List(rene, gerard)

  feature("Any authenticated user must be able to create a new paper with the \\BlueLaTeX service") {

    scenario("a successful paper creation") {

      Given("a an authenticated user person")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> rene.username, "password" -> rene.password))

      loggedin should be(true)

      When("he creates a new paper")
      val title = "On Writing Tests"
      val (id, _) = post[String](List("papers"), Map("paper_name" -> title, "paper_title" -> title))

      Then("the paper can be retrieved from the service")
      val (paper, _) = get[PaperInfo](List("papers", id, "info"))

      paper.name should be(title)

      val (roles, _) = get[PaperRole](List("papers", id, "roles"))

      roles.authors should be(Set(rene.username))
      roles.reviewers should be('empty)

      When("the user logs out")
      val (loggedout, _) = delete[Boolean](List("session"))

      loggedout should be(true)

      Then("he can no longer access the created public paper")
      val exn = evaluating {
        get[Int](List("papers", id, "compiled", "pages"))
      } should produce[BlueErrorException]

      exn.status should be(403)

    }

    scenario("a public paper creation") {

      Given("a an authenticated user person")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> gerard.username, "password" -> gerard.password))

      loggedin should be(true)

      When("he creates a new paper")
      val title = "On Writing Tests"
      val (id, _) = post[String](List("papers"), Map("paper_name" -> title, "paper_title" -> title, "visibility" -> "public"))

      Then("the paper can be retrieved from the service")
      val (paper, _) = get[PaperInfo](List("papers", id, "info"))

      paper.name should be(title)

      val (roles, _) = get[PaperRole](List("papers", id, "roles"))

      roles.authors should be(Set(gerard.username))
      roles.reviewers should be('empty)

      When("the user logs out")
      val (loggedout, _) = delete[Boolean](List("session"))

      loggedout should be(true)

      Then("He can still access the created public paper")
      val exn = evaluating {
        get[Int](List("papers", id, "compiled", "pages"))
      } should produce[BlueErrorException]

      exn.status should be(404)

    }

  }

}

