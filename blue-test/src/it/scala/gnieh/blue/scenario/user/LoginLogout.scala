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

/** Scenarii to the login/logout of a user
 *   - correct login
 *   - login with wrong password
 *   - login with missing password
 *   - login with missing username
 *   - login of unknown user
 *   - correct logout
 *   - logout when not logged in
 *
 *  @author Lucas Satabin
 */
class UserLoginLogoutSpec extends BlueScenario with SomeUsers {

  val predefinedPeople =
    List(Person("glambert", "Gérard", "Lambert", "gerard@lambert.org", Some("Gnieh Inc.")))

  feature("Any registered user must be able to log into the \\BlueLaTeX service"){

    info("Once people are registered in \\BlueLaTeX right management is based on identified users")

    scenario("successful login") {

      Given("a person")
      val person = predefinedPeople.head

      When("he is not logged in")
      val exc = evaluating {
        get[UserInfo](List("session"))
      } should produce[BlueErrorException]
      exc.status should be(401)

      And("he sends a valid login request")
      val loggedin = post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

      loggedin should be(true)

      Then("he is logged in")
      val info = get[UserInfo](List("session"))

      info.name should be("glambert")
      info.roles.head should be("blue_user")

      When("he sends a valid logout request")
      val loggedout = delete[Boolean](List("session"))

      loggedout should be(true)

      Then("he is logged out")
      val exc2 = evaluating {
        get[UserInfo](List("session"))
      } should produce[BlueErrorException]
      exc2.status should be(401)

    }

    scenario("wrong password") {

      Given("a person")
      val person = predefinedPeople.head

      When("he is not logged in")
      val exc = evaluating {
        get[UserInfo](List("session"))
      } should produce[BlueErrorException]
      exc.status should be(401)

      And("he sends a login request with wrong password")
      val exc2 = evaluating {
        post[Boolean](List("session"), Map("username" -> person.username, "password" -> (person.password + "_invalid")))
      } should produce[BlueErrorException]
      exc2.status should be(401)
      val error = exc2.error
      error.name should be("unable_to_login")
      error.description should be("Wrong username and/or password")

    }

  }

}

