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

/** Scenarii to test the user registration feature:
 *   - registration of new user
 *   - registration with missing parameters
 *   - registration of an already existing user identifier
 *
 *  @author Lucas Satabin
 */
class UserRegistrationSpec extends BlueScenario {

  feature("Any person must be able to register with the \\BlueLaTeX service"){

    info("The \\BlueLaTeX service requires the users to be registered")
    info("Thus, the service must provide a way to the user to register")

    scenario("a successful user registration") {

      Given("a person")
      val person = Person("glambert", "Gerard", "Lambert", "gerard@lambert.org", Some("Gnieh Inc."))

      When("she sends a valid registration request to the server")
      val registered = post[Boolean](List("users"), person.toMap)

      registered should be(true)

      Then("she receives a confirmation email")
      val email = mailbox.lastMailFor(person.email_address).getOrElse(fail("No email received"))
      val token = email match {
        case PasswordResetRegex(name, token) if name == person.username => token
        case _                                                          => fail("Received an invalid validation email")
      }

      And("must validate her account by reseting her password")
      val reset = post[Boolean](List("users", person.username, "reset"),
        Map(
          "reset_token" -> token,
          "new_password1" -> person.password,
          "new_password2" -> person.password
        )
      )

      reset should be(true)

      Then("she can log into the service with this password")
      val loggedin = post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

      loggedin should be(true)

    }

    scenario("a user registration with missing parameters") {

      Given("a person")

      When("she sends a registration request with not username")

      Then("she receives an error response")

    }

    scenario("a user registration for an already existing name") {

      Given("a person")

      When("she sends a registration request with an already existing username")

      Then("she receives an error response")

    }

  }

}
