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
package compiler

import org.scalatest._

import http.ErrorResponse

/** Scenarios for the compilation of a paper:
 *   - non author is not allowed to register to compilation stream
 */
class CompilationSpec extends BlueScenario with SomeUsers with SomePapers {

  val predefinedPapers = List(paper1, paper2)

  val predefinedPeople = List(gerard, prince)

  feature("An author of a paper must be able to register to the compilation stream of his papers") {

    scenario("An author should be able to register to compilation stream") {

      Given("an authenticated person")
      val (loggedin, _) = login(gerard)

      loggedin should be(true)

      When("he tries to register to a compilation stream")
      val exn = evaluating {
        post[Boolean](List("papers", paper1._id, "compiler"), Map(), headers = Map("Content-Length" -> "0", "Content-Type" -> "application/json"))
      } should produce[BlueErrorException]

      Then("the server authorizes the registration")
      exn.status should be(503)
      val error = exn.error
      error.name should be("unable_to_compile")
      error.description should be("No compilation task started")

    }

    scenario("A reviewer should be able to register to compilation stream") {

      Given("an authenticated person")
      val (loggedin, _) = login(gerard)

      loggedin should be(true)

      When("he tries to register to a compilation stream")
      val exn = evaluating {
        post[Boolean](List("papers", paper2._id, "compiler"), Map(), headers = Map("Content-Length" -> "0", "Content-Type" -> "application/json"))
      } should produce[BlueErrorException]

      Then("the server authorizes the registration")
      exn.status should be(503)
      val error = exn.error
      error.name should be("unable_to_compile")
      error.description should be("No compilation task started")

    }

    scenario("An unauthenticated user cannot register to a compilation stream") {

      Given("an unauthenticated person")
      When("he tries to register to a compilation stream")
      val exn = evaluating {
        post[Boolean](List("papers", paper1._id, "compiler"), Map(), headers = Map("Content-Length" -> "0", "Content-Type" -> "application/json"))
      } should produce[BlueErrorException]

      Then("he receives an error message")
      exn.status should be(403)
      val error = exn.error
      error.name should be("no_sufficient_rights")
      error.description should be("You have no permission to compile the paper")

    }

    scenario("A person not involved in a paper cannot register to its compilation stream") {

      Given("an authenticated person")
      val (loggedin, _) = login(prince)

      loggedin should be(true)

      When("he tries to register to a compilation stream of a paper in which he is not involved at all (not author nor reviewer)")
      val exn = evaluating {
        post[Boolean](List("papers", paper1._id, "compiler"), Map(), headers = Map("Content-Length" -> "0", "Content-Type" -> "application/json"))
      } should produce[BlueErrorException]

      Then("he receives an error message")
      exn.status should be(403)
      val error = exn.error
      error.name should be("no_sufficient_rights")
      error.description should be("You have no permission to compile the paper")

    }

  }

}

