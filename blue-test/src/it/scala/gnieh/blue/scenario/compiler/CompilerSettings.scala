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

import gnieh.sohva.UserInfo
import gnieh.diffson._

import http.ErrorResponse
import couch.Paper

/** Scenarios for the compiler settings management:
 *   - author retrieving existing settings for a compiler,
 *   - author saving settings for a compiler,
 *   - non author trying to retrieve settings for a compiler,
 *   - non author trying to save settings for a compiler
 */
class CompilerSettingsSpec extends BlueScenario with SomeUsers with SomePapers {

  val gerard = Person("glambert", "Gérard", "Lambert", "gerard@lambert.org", Some("Gnieh Inc."))
  val prince = Person("pprince", "Petit", "Prince", "petit@prince.org", None)

  val predefinedPapers: List[Paper] =
    List(Paper("paper1", "Some Test Paper", Set("glambert"), Set("pprince"), "article"))

  val predefinedPeople: List[Person] =
    List(gerard, prince)

  feature("Any paper author must be able to view and change the compiler settings") {

    scenario("Successful retrieval") {

      Given("an authenticated person")
      val loggedin = post[Boolean](List("session"), Map("username" -> gerard.username, "password" -> gerard.password))

      loggedin should be(true)

      When("he saves the settings for a paper on which he is an author")
      val p =
        JsonPatch.parse("""[
                          |  {
                          |    "op":"add",
                          |    "path":"/compiler",
                          |    "value":"pdflatex"
                          |  },{
                          |    "op":"add",
                          |    "path":"/interval",
                          |    "value":15
                          |  },{
                          |    "op":"add",
                          |    "path":"/timeout",
                          |    "value":30
                          |  }
                          |]""".stripMargin)
      val saved = patch[Boolean](List("papers", "paper1", "compiler", "settings"), p)

      saved should be(true)

    }

  }

}

