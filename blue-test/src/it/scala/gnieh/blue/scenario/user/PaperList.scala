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
package user

import org.scalatest._

import gnieh.sohva.UserInfo

import http.ErrorResponse
import couch._

/** Scenarios to test user papers listing:
 *   - listing for authenticated user with several papers
 *   - listing for authenticated user with no papers
 *   - listing for unauthenticated user
 *
 *  @author Lucas Satabin
 */
class PaperListSpec extends BlueScenario with SomeUsers with SomePapers {

  val gerard = Person("glambert", "Gérard", "Lambert", "gerard@lambert.org", Some("Gnieh Inc."))
  val prince = Person("pprince", "Petit", "Prince", "petit@prince.org", None)

  val predefinedPeople =
    List(gerard, prince)

  val paper1 = Paper("paper1", "Some Paper", Set("glambert"), Set(), "article")
  val paper2 = Paper("paper2", "Some Other Paper", Set("toto"), Set("glambert"), "article")

  val predefinedPapers =
    List(paper1, paper2)

  feature("Any authenticated user must be able retrieve the list of papers he is involved in"){

    scenario("non empty list of papers") {

      Given("an authenticated person")
      val loggedin = post[Boolean](List("session"), Map("username" -> gerard.username, "password" -> gerard.password))

      loggedin should be(true)

      When("he asks for his list of papers")
      val papers = get[List[UserRole]](List("users", gerard.username, "papers"))

      Then("he should receive the list of papers he is involved in with his role")
      papers.size should be(2)
      papers should contain(UserRole("paper1", "Some Paper", "author"))
      papers should contain(UserRole("paper2", "Some Other Paper", "reviewer"))

    }

    scenario("non empty list of papers for other user") {

      Given("an authenticated person")
      val loggedin = post[Boolean](List("session"), Map("username" -> prince.username, "password" -> prince.password))

      loggedin should be(true)

      When("he asks for the list of papers for another person")
      val papers = get[List[UserRole]](List("users", gerard.username, "papers"))

      Then("he should receive the list of papers this other person is involved in with his role")
      papers.size should be(2)
      papers should contain(UserRole("paper1", "Some Paper", "author"))
      papers should contain(UserRole("paper2", "Some Other Paper", "reviewer"))

    }

    scenario("empty list of papers") {

      Given("an authenticated person")
      val loggedin = post[Boolean](List("session"), Map("username" -> prince.username, "password" -> prince.password))

      loggedin should be(true)

      When("he asks for his list of papers")
      val papers = get[List[UserRole]](List("users", prince.username, "papers"))

      Then("he should receive an empty list if he is not involved in any paper")
      papers.size should be(0)

    }

    scenario("unauthenticated user") {

      Given("an unauthenticated person")
      val exc1 = evaluating {
        get[UserInfo](List("session"))
      } should produce[BlueErrorException]
      exc1.status should be(401)

      When("he asks for a list of papers")
      val exc2 = evaluating {
        get[List[UserRole]](List("users", prince.username, "papers"))
      } should produce[BlueErrorException]

      Then("he should receive an empty list if he is not involved in any paper")
      exc2.status should be(401)

    }

  }

}

