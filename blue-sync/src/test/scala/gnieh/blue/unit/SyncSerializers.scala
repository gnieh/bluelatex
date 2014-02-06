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
package sync
package impl
package unit

import org.scalatest._

import net.liftweb.json._
import net.liftweb.json.Serialization

class SyncSerializerParsersSpec extends FeatureSpec
                                with BeforeAndAfterAll
                                with GivenWhenThen
                                with ShouldMatchers {

  private implicit val formats = DefaultFormats +
                                 new SyncSessionSerializer +
                                 new SyncCommandSerializer +
                                 new MessageSerializer +
                                 new SyncActionSerializer +
                                 new EditSerializer

  def formatJson(json: String): String = compact(render(parse(json)))

  feature("Edit commands parser should correctly parse commands") {

    scenario("Add parsing") {
      Given("an add representation")
      val add = "+-A-Za-z0-9_.!~*'();/?:@&=+$,# %0F"

      When("it is parsed")
      val result = EditCommandParsers.parseEdits(add)

      Then("an add message is expected")
      result should be (List(Add("-A-Za-z0-9_.!~*'();/?:@&=+$,# %0F")))
    }

    scenario("Delete parsing") {
      Given("a delete representation")
      val del = "-10"

      When("it is parsed")
      val result = EditCommandParsers.parseEdits(del)

      Then("an delete message is expected")
      result should be (List(Delete(10)))
    }

    scenario("Equality parsing") {
      Given("a equality representation")
      val eq = "=10"

      When("it is parsed")
      val result = EditCommandParsers.parseEdits(eq)

      Then("an equality message is expected")
      result should be (List(Equality(10)))
    }

    scenario("Multiple parsing") {
      Given("a composed edit")
      val edits = "=5\t+Hello"

      When("it is parsed")
      val result = EditCommandParsers.parseEdits(edits)

      Then("an equality message is expected")
      result should be (List(Equality(5), Add("Hello")))
    }

  }

  feature("SyncSerializer should correctly parse a JSON message to a SyncSession object") {

    scenario("Wiki example") {
      Given("a JSON message")
      val json = """|{
                    |  "peerId": "toto",
                    |  "paperId": "v987fed987da70987f",
                    |  "commands": [
                    |    {
                    |      "filename": "file1.tex",
                    |      "revision": 432767,
                    |      "action": {
                    |        "name": "delta",
                    |        "revision": 4324523,
                    |        "data": [ "=100", "-2", "+test", "=34" ],
                    |        "overwrite": false
                    |      }
                    |    },
                    |    {
                    |      "filename": "file2.tex",
                    |      "revision": 87432,
                    |      "action": {
                    |       "name": "raw",
                    |       "revision": 4324523,
                    |       "data": "Complete raw text",
                    |       "overwrite": false
                    |      }
                    |    }
                    |  ]
                    |}""".stripMargin

      When("serialized to a scala object")
      val syncSession = Serialization.read[SyncSession](json)

      Then("a correct SyncSession object shall be produced")
      syncSession should be (SyncSession("toto", "v987fed987da70987f",
                                         List(SyncCommand("file1.tex", 432767,
                                                          Delta(4324523, List(Equality(100),
                                                                              Delete(2),
                                                                              Add("test"),
                                                                              Equality(34)),
                                                                false)),
                                              SyncCommand("file2.tex", 87432,
                                                          Raw(4324523, "Complete raw text", false)))))
    }

    scenario("Broadcast messages") {
      Given("a JSON message")
      val json = """|{
                    |  "peerId": "toto",
                    |  "paperId": "v987fed987da70987f",
                    |  "commands": [
                    |    {
                    |      "from": "someone",
                    |      "json": {
                    |        "content": "Hello all"
                    |      },
                    |      "retrieve": false
                    |    },
                    |    {
                    |      "from": "whale",
                    |      "json": {
                    |        "content": "Thanks for the fish"
                    |      },
                    |      "retrieve": true
                    |    }
                    |  ]
                    |}""".stripMargin

      When("serialized to a scala object")
      val syncSession = Serialization.read[SyncSession](json)

      Then("a correct SyncSession object shall be produced")
      syncSession should be (SyncSession("toto", "v987fed987da70987f",
                                         List(Message("someone",
                                                      JObject(List(JField("content",
                                                                          JString("Hello all")))),
                                                      false, None),
                                              Message("whale",
                                                      JObject(List(JField("content",
                                                                          JString("Thanks for the fish")))),
                                                      true, None))))
    }

    scenario("Broadcast messages with filename") {
      Given("a JSON message")
      val json = """|{
                    |  "peerId": "toto",
                    |  "paperId": "v987fed987da70987f",
                    |  "commands": [
                    |    {
                    |      "from": "Sherlock",
                    |      "json": {
                    |        "content": "I'm not a psychopath, I'm a high-functioning sociopath"
                    |      },
                    |      "retrieve": false,
                    |      "filename": "quote.tex"
                    |    }
                    |  ]
                    |}""".stripMargin

      When("serialized to a scala object")
      val syncSession = Serialization.read[SyncSession](json)

      Then("a correct SyncSession object shall be produced")
      syncSession should be (SyncSession("toto", "v987fed987da70987f",
                                         List(Message("Sherlock",
                                                      JObject(List(JField("content",
                                                                          JString("I'm not a psychopath, I'm a high-functioning sociopath")))),
                                                      false, Some("quote.tex")))))
    }
  }

  feature("SyncSerializer should correctly serialize a SyncSession object to a JSON message") {

    scenario("Wiki example") {
      Given("a SyncSession Object")
      val syncSession = SyncSession("toto", "v987fed987da70987f",
                                    List(SyncCommand("file1.tex", 432767,
                                                     Delta(4324523, List(Equality(100),
                                                                         Delete(2),
                                                                         Add("test"),
                                                                         Equality(34)),
                                                           false)),
                                          SyncCommand("file2.tex", 87432,
                                                      Raw(4324523, "Complete raw text", false))))

      When("serialized to JSON")
      val serializedSyncSession = Serialization.write(syncSession)

      Then("a correct SyncSession object shall be produced")
      val json = """|{
                    |  "peerId": "toto",
                    |  "paperId": "v987fed987da70987f",
                    |  "commands": [
                    |    {
                    |      "filename": "file1.tex",
                    |      "revision": 432767,
                    |      "action": {
                    |        "name": "delta",
                    |        "revision": 4324523,
                    |        "data": [ "=100", "-2", "+test", "=34" ],
                    |        "overwrite": false
                    |      }
                    |    },
                    |    {
                    |      "filename": "file2.tex",
                    |      "revision": 87432,
                    |      "action": {
                    |       "name": "raw",
                    |       "revision": 4324523,
                    |       "data": "Complete raw text",
                    |       "overwrite": false
                    |      }
                    |    }
                    |  ]
                    |}""".stripMargin
        formatJson(json) should be (formatJson(serializedSyncSession))
    }

    scenario("Broadcast messages") {
      Given("a SyncSession Object")
      val syncSession = SyncSession("toto", "v987fed987da70987f",
                                    List(Message("someone",
                                                 JObject(List(JField("content",
                                                                     JString("Hello all")))),
                                                 false, None),
                                         Message("whale",
                                                 JObject(List(JField("content",
                                                                     JString("Thanks for the fish")))),
                                                 true, None)))

      When("serialized to a JSON message")
      val serializedSyncSession = Serialization.write(syncSession)

      Then("a correct JSON message shall be produced")
      val json = """|{
                    |  "peerId": "toto",
                    |  "paperId": "v987fed987da70987f",
                    |  "commands": [
                    |    {
                    |      "from": "someone",
                    |      "json": {
                    |        "content": "Hello all"
                    |      },
                    |      "retrieve": false
                    |    },
                    |    {
                    |      "from": "whale",
                    |      "json": {
                    |        "content": "Thanks for the fish"
                    |      },
                    |      "retrieve": true
                    |    }
                    |  ]
                    |}""".stripMargin
      formatJson(json) should be (formatJson(serializedSyncSession))
    }

    scenario("Broadcast messages with filename") {

      Given("a SyncSession object")
      val syncSession = SyncSession("toto", "v987fed987da70987f",
                                    List(Message("Sherlock",
                                                 JObject(List(JField("content",
                                                                     JString("I'm not a psychopath, I'm a high-functioning sociopath")))),
                                                 false, Some("quote.tex"))))

      When("serialized to a JSON message")
      val serializedSyncSession = Serialization.write(syncSession)

      Then("a valid JSON message should be produced")
      val json = """|{
                    |  "peerId": "toto",
                    |  "paperId": "v987fed987da70987f",
                    |  "commands": [
                    |    {
                    |      "from": "Sherlock",
                    |      "json": {
                    |        "content": "I'm not a psychopath, I'm a high-functioning sociopath"
                    |      },
                    |      "retrieve": false,
                    |      "filename": "quote.tex"
                    |    }
                    |  ]
                    |}""".stripMargin
      formatJson(json) should be (formatJson(serializedSyncSession))

    }
  }
}
