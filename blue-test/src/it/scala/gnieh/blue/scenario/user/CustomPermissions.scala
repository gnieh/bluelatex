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
import OptionValues._

import gnieh.sohva.UserInfo

import http.ErrorResponse

import gnieh.diffson.JsonDiff

/** Scenarios to test custom permissions management:
 *   - default permissions
 *   - overriding built-in permissions
 *   - adding custom permissions
 *   - deleting custom permissions
 *   - deleting overriding permissions
 *
 *  @author Lucas Satabin
 */
class UserPermissionsSpec extends BlueScenario with SomeUsers {

  val predefinedPeople =
    List(gerard, rene)

  feature("Any logged in user must be able to manage a list of available permissions"){

    info("Once people are logged into \\BlueLaTeX, available permissions are dependent on identified users")

    scenario("default permissions") {

      Given("a new user")
      val person = predefinedPeople.head

      When("he is logged in")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

      loggedin should be(true)

      Then("he can access default permission sets (private and public)")
      val (perms, _) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      perms.size should be(2)
      perms.keys should contain("private")
      perms.keys should contain("public")

      val (loggedout, _) = delete[Boolean](List("session"))

      loggedout should be(true)

    }

    scenario("overriding default permission sets") {

      Given("a new person")
      val person = predefinedPeople.head

      When("he is logged in")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

      loggedin should be(true)

      When("he overwrites the default public permissions")
      val (perms, headers) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      headers.get("ETag") should not be('defined)

      val perms1 = perms.updated("public", perms("public").updated("anonymous", Set()))
      val p = JsonDiff.diff(perms, perms1)

      val (saved, _) = patch[Boolean](List("users", person.username, "permissions"), p, "")

      saved should be(true)

      Then("he can retrieve the new permission sets")
      val (perms2, _) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      perms2 should not be(perms)
      perms2 should be(perms1)

      val (loggedout, _) = delete[Boolean](List("session"))

      loggedout should be(true)

    }

    scenario("adding new permission sets") {

      Given("a new person")
      val person = predefinedPeople.head

      When("he is logged in")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

      loggedin should be(true)

      When("he adds new permission sets")
      val (perms, headers) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      headers.get("ETag") should not be('defined)

      val perms1 = perms
        .updated("custom1", Map("author" -> Set("read")))
        .updated("custom2", Map("reviewer" -> Set("read")))

      val p = JsonDiff.diff(perms, perms1)

      val (saved, _) = patch[Boolean](List("users", person.username, "permissions"), p, "")

      saved should be(true)

      Then("he can retrieve the newly created permission sets")
      val (perms2, _) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      perms2 should not be(perms)
      perms2 should be(perms1)

      val (loggedout, _) = delete[Boolean](List("session"))

      loggedout should be(true)

    }

    scenario("deleting custom permission sets") {

      Given("a new person")
      val person = predefinedPeople.head

      When("he is logged in")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

      loggedin should be(true)

      When("he adds new permission sets")
      val (perms, headers) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      headers.get("ETag") should not be('defined)

      val perms1 = perms
        .updated("custom1", Map("author" -> Set("read")))
        .updated("custom2", Map("reviewer" -> Set("read")))

      val p = JsonDiff.diff(perms, perms1)

      val (saved, _) = patch[Boolean](List("users", person.username, "permissions"), p, "")

      saved should be(true)

      Then("he can retrieve the newly created permission sets")
      val (perms2, headers1) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      headers1.get("ETag") should be('defined)
      headers1.get("ETag").value.size should be(1)

      val etag = headers1("ETag").head

      perms2 should not be(perms)
      perms2 should be(perms1)

      And("he can delete a new permission set")
      val perms3 = perms2 - "custom2"
      val p1 = JsonDiff.diff(perms2, perms3)

      val (saved1, _) = patch[Boolean](List("users", person.username, "permissions"), p1, etag)

      saved1 should be(true)

      Then("this permission no longer exists")
      val (perms4, _) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      perms4 should not be(perms)
      perms4 should not be(perms1)
      perms4 should not be(perms2)
      perms4 should be(perms3)

      val (loggedout, _) = delete[Boolean](List("session"))

      loggedout should be(true)

    }

    scenario("deleting overridden default permission sets") {

      Given("a new person")
      val person = predefinedPeople.head

      When("he is logged in")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

      loggedin should be(true)

      When("he overwrites the default public permissions")
      val (perms, headers) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      headers.get("ETag") should not be('defined)

      val perms1 = perms.updated("public", perms("public").updated("anonymous", Set()))
      val p = JsonDiff.diff(perms, perms1)

      val (saved, _) = patch[Boolean](List("users", person.username, "permissions"), p, "")

      saved should be(true)

      Then("he can retrieve the new permission sets")
      val (perms2, headers1) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      headers1.get("ETag") should be('defined)
      headers1.get("ETag").value.size should be(1)

      val etag = headers1("ETag").head
      perms2 should not be(perms)
      perms2 should be(perms1)

      And("he can then delete the overriding")
      val perms3 = perms2 - "public"

      val p1 = JsonDiff.diff(perms2, perms3)

      val (saved1, _) = patch[Boolean](List("users", person.username, "permissions"), p1, etag)

      saved1 should be(true)

      Then("this permission no longer exists")
      val (perms4, _) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      perms4 should be(perms)
      perms4 should not be(perms1)
      perms4 should not be(perms2)
      perms4 should not be(perms3)

      val (loggedout, _) = delete[Boolean](List("session"))

      loggedout should be(true)

    }

    scenario("local modifications") {

      Given("a new person")
      val person = predefinedPeople.head

      When("he is logged in")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

      loggedin should be(true)

      When("he adds new permission sets")
      val (perms, headers) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      headers.get("ETag") should not be('defined)

      val perms1 = perms
        .updated("custom1", Map("author" -> Set("read")))
        .updated("custom2", Map("reviewer" -> Set("read")))

      val p = JsonDiff.diff(perms, perms1)

      val (saved, _) = patch[Boolean](List("users", person.username, "permissions"), p, "")

      saved should be(true)

      Then("he can retrieve the newly created permission sets")
      val (perms2, _) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      perms2 should not be(perms)
      perms2 should be(perms1)

      val (loggedout, _) = delete[Boolean](List("session"))

      loggedout should be(true)

      Given("another user")
      val person1 = predefinedPeople.tail.head

      When("he is logged in")
      val (loggedin1, _) = post[Boolean](List("session"), Map("username" -> person1.username, "password" -> person1.password))

      loggedin1 should be(true)

      And("retrieves the list of permission sets")
      val (perms3, headers1) = get[Map[String,Map[String,Set[String]]]](List("users", person.username, "permissions"))

      headers1.get("ETag") should not be('defined)

      Then("he does not see the custom permission sets of other users")

      perms3.keys should not contain("custom1")
      perms3.keys should not contain("custom2")
      perms3.keys should contain("public")
      perms3.keys should contain("private")


    }

  }

}
