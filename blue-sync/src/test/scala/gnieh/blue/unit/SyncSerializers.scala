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
      val result = EditCommandParsers.parseEdit(add)

      Then("an add message is expected")
      result should be (Some(Add("-A-Za-z0-9_.!~*'();/?:@&=+$,# %0F")))
    }

    scenario("Delete parsing") {
      Given("a delete representation")
      val del = "-10"

      When("it is parsed")
      val result = EditCommandParsers.parseEdit(del)

      Then("an delete message is expected")
      result should be (Some(Delete(10)))
    }

    scenario("Equality parsing") {
      Given("a equality representation")
      val eq = "=10"

      When("it is parsed")
      val result = EditCommandParsers.parseEdit(eq)

      Then("an equality message is expected")
      result should be (Some(Equality(10)))
    }

    scenario("Long edit command") {
      Given("a long text")
      val text = "Et licet quocumque oculos flexeris feminas adfatim multas spectare cirratas, quibus, si nupsissent, per aetatem ter iam nixus poterat suppetere liberorum, ad usque taedium pedibus pavimenta tergentes iactari volucriter gyris, dum exprimunt innumera simulacra, quae finxere fabulae theatrales.\n\nHaec subinde Constantius audiens et quaedam referente Thalassio doctus, quem eum odisse iam conpererat lege communi, scribens ad Caesarem blandius adiumenta paulatim illi subtraxit, sollicitari se simulans ne, uti est militare otium fere tumultuosum, in eius perniciem conspiraret, solisque scholis iussit esse contentum palatinis et protectorum cum Scutariis et Gentilibus, et mandabat Domitiano, ex comite largitionum, praefecto ut cum in Syriam venerit, Gallum, quem crebro acciverat, ad Italiam properare blande hortaretur et verecunde.\nSed si ille hac tam eximia fortuna propter utilitatem rei publicae frui non properat, ut omnia illa conficiat, quid ego, senator, facere debeo, quem, etiamsi ille aliud vellet, rei publicae consulere oporteret?\nEt licet quocumque oculos flexeris feminas adfatim multas spectare cirratas, quibus, si nupsissent, per aetatem ter iam nixus poterat suppetere liberorum, ad usque taedium pedibus pavimenta tergentes iactari volucriter gyris, dum exprimunt innumera simulacra, quae finxere fabulae theatrales.\n\nHaec subinde Constantius audiens et quaedam referente Thalassio doctus, quem eum odisse iam conpererat lege communi, scribens ad Caesarem blandius adiumenta paulatim illi subtraxit, sollicitari se simulans ne, uti est militare otium fere tumultuosum, in eius perniciem conspiraret, solisque scholis iussit esse contentum palatinis et protectorum cum Scutariis et Gentilibus, et mandabat Domitiano, ex comite largitionum, praefecto ut cum in Syriam venerit, Gallum, quem crebro acciverat, ad Italiam properare blande hortaretur et verecunde.\n\nSed si ille hac tam eximia fortuna propter utilitatem rei publicae frui non properat, ut omnia illa conficiat, quid ego, senator, facere debeo, quem, etiamsi ille aliud vellet, rei publicae consulere oporteret?\nEt licet quocumque oculos flexeris feminas adfatim multas spectare cirratas, quibus, si nupsissent, per aetatem ter iam nixus poterat suppetere liberorum, ad usque taedium pedibus pavimenta tergentes iactari volucriter gyris, dum exprimunt innumera simulacra, quae finxere fabulae theatrales.\n\nHaec subinde Constantius audiens et quaedam referente Thalassio doctus, quem eum odisse iam conpererat lege communi, scribens ad Caesarem blandius adiumenta paulatim illi subtraxit, sollicitari se simulans ne, uti est militare otium fere tumultuosum, in eius perniciem conspiraret, solisque scholis iussit esse contentum palatinis et protectorum cum Scutariis et Gentilibus, et mandabat Domitiano, ex comite largitionum, praefecto ut cum in Syriam venerit, Gallum, quem crebro acciverat, ad Italiam properare blande hortaretur et verecunde.\n\nSed si ille hac tam eximia fortuna propter utilitatem rei publicae frui non properat, ut omnia illa conficiat, quid ego, senator, facere debeo, quem, etiamsi ille aliud vellet, rei publicae consulere oporteret?\n"

      And("an Edit containing this text")
      val edits = List("=300", s"+${text}", "-20")

      When("it is parsed")
      val result = edits.map { e => EditCommandParsers.parseEdit(e) }.flatten

      Then("a correct Edit list is expected")
      result should be (List(Equality(300), Add(text), Delete(20)))
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
                    |      }
                    |    },
                    |    {
                    |      "from": "whale",
                    |      "json": {
                    |        "content": "Thanks for the fish"
                    |      }
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
                                                      None),
                                              Message("whale",
                                                      JObject(List(JField("content",
                                                                          JString("Thanks for the fish")))),
                                                      None))))
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
                                                      Some("quote.tex")))))
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
                                                 None),
                                         Message("whale",
                                                 JObject(List(JField("content",
                                                                     JString("Thanks for the fish")))),
                                                 None)))

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
                    |      }
                    |    },
                    |    {
                    |      "from": "whale",
                    |      "json": {
                    |        "content": "Thanks for the fish"
                    |      }
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
                                                 Some("quote.tex"))))

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
                    |      "filename": "quote.tex"
                    |    }
                    |  ]
                    |}""".stripMargin
      formatJson(json) should be (formatJson(serializedSyncSession))

    }
  }
}
