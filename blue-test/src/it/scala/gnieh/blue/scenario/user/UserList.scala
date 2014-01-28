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

/** Scenarios to test the user list feature:
 *   - authenticated user get all users
 *   - authenticated user get filtered users
 *   - unauthenticated user get filtered users
 *
 *  @author Lucas Satabin
 */
class UserListSpec extends BlueScenario with SomeUsers {

  val alan = Person("aturing", "Alan", "Turing", "alan@turing.org", None)

  val alonzo = Person("achurch", "Alonzo", "Church", "alonzo@church.org", None)

  val predefinedPeople = List(gerard, rene, prince, alan, alonzo)

  feature("Any authenticated user must be able to retrieve a list of users") {

    scenario("An authenticated user must be able to retrieve the entire list of users") {

      val (loggedin, _) = login(gerard)

      loggedin should be(true)

      val (users, _) = get[List[String]](List("users"))

      users should have size(5)
      users should be(predefinedPeople.map(_.username).sorted)

    }

    scenario("An authenticated user must be able to retrieve a filtered list of users") {

      val (loggedin, _) = login(gerard)

      loggedin should be(true)

      val (users, _) = get[List[String]](List("users"), parameters = Map("name" -> "a"))

      users should have size(2)
      users should be(List("achurch", "aturing"))

    }

  }

  feature("Unauthenticated people must not be able to retrieve list of users") {

    scenario("An unauthenticated user must not be able to retrieve the entire list of users") {

      val exn = evaluating {
        get[List[String]](List("users"))
      } should produce[BlueErrorException]

      exn.status should be(401)

    }

    scenario("An unauthenticated user must not be able to retrieve a filtered list of users") {

      val exn = evaluating {
        get[List[String]](List("users"), parameters = Map("name" -> "a"))
      } should produce[BlueErrorException]

      exn.status should be(401)

    }

  }

}

