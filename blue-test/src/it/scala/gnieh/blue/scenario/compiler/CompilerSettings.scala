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
class CompilerSettingsSpec extends BlueScenario with SomeUsers {

  val predefinedPeople: List[Person] =
    List(gerard, prince)

  feature("Any paper author must be able to view and change the compiler settings") {

    scenario("Successful retrieval") {

      Given("an authenticated person")
      val (loggedin, _) = post[Boolean](List("session"), Map("username" -> gerard.username, "password" -> gerard.password))

      loggedin should be(true)

      When("he creates a new paper")
      val title = "Some Test Paper"
      val (paperId, _) = post[String](List("papers"), Map("paper_title" -> title))

      Then("settings are created for the newly created paper")
      val (compilerSettings, headers) = get[CompilerSettings](List("papers", paperId, "compiler"))

      compilerSettings.compiler should be("pdflatex")
      compilerSettings.timeout should be(30)
      compilerSettings.interval should be(15)
      headers.get("ETag") should be('defined)

      val revision = headers("ETag").head

      And("he can modify and save these settings")
      val newSettings = compilerSettings.copy(compiler = "xelatex")

      val p = JsonDiff.diff(compilerSettings, newSettings)

      val (saved, _) = patch[Boolean](List("papers", paperId, "compiler"), p, revision)

      saved should be(true)

      Then("the new settings are the one taken into account")
      val (newCompilerSettings, newHeaders) = get[CompilerSettings](List("papers", paperId, "compiler"))

      newCompilerSettings.compiler should be("xelatex")
      newCompilerSettings.timeout should be(30)
      newCompilerSettings.interval should be(15)
      newHeaders.get("ETag") should be('defined)

      newHeaders("ETag") should not be(revision)

    }

  }

}

