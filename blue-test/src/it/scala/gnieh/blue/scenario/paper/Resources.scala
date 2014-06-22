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

import scala.util.Random

import java.io.File

/** Scenarios for resource management
 *
 *  @author Lucas Satabin
 */
class SaveResource extends BlueScenario with SomeUsers {

  val user = Person("lsatabin", "Lucas", "Satabin", "lucas@satabin.plop", None)

  val predefinedPeople =
    List(user)

  feature("When a user creates a new paper, he should be able to save resources for this paper") {

    scenario("save resource") {

      Given("an authenticated user")
      val (loggedin, _) = login(user)

      loggedin should be(true)

      When("he creates a new paper")
      val title = "On Writing Tests"
      val (paperid, _) = post[String](List("papers"), Map("paper_name" -> title, "paper_title" -> title))

      val hddFile = new File("src/it/resources/resource.txt")

      Then("he can upload a file")
      val (saved, _) =
        postFile[Boolean](List("papers", paperid, "files", "resources", "resource.txt"), hddFile, "text/plain")

      saved should be(true)

      And("the resource was added")
      val (resources, _) = get[List[String]](List("papers", paperid, "files", "resources"))

      resources should be(List("resource.txt"))

      And("he can retrieve it")
      val (file, headers) = getRaw(List("papers", paperid, "files", "resources", "resource.txt"))

      file should be("This is an example text file to upload\n")
      headers.get("Content-Type") should be(Some(List("text/plain")))

    }

  }

}

