
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
package compat
package unit

import org.scalatest._

import ProtocolTranslator._

import net.liftweb.json.JsonDSL._

/** Tests the compatibility conversions between mobwrite and blue-sync messages.
 */
class CompatibilityTests extends FlatSpec with ShouldMatchers {

  "converting a mobwrite session to blue-sync session" should "be correct" in {
    val mobwrite = """u:fraser
                     |F:34:file1.tex
                     |d:41:=200	-7	+Hello	=100
                     |F:53:file2.tex
                     |d:90:=123	+Fool	=296
                     |""".stripMargin
    val paperId = "paper_id"

    val session =
      SyncSession(
        "fraser",
        "paper_id",
        List(
          SyncCommand(
            "file1.tex",
            34,
            Delta(
              41,
              List(Equality(200), Delete(7), Add("Hello"), Equality(100)),
              false
            )
          ),
          SyncCommand(
            "file2.tex",
            53,
            Delta(
              90,
              List(Equality(123), Add("Fool"), Equality(296)),
              false
            )
          )
        )
      )

    val converted = mobwrite2bluelatex(paperId, mobwrite)

    converted should be(List(session))

  }

  "converting a blue-sync session into mobwrite messages" should "be correct" in {

    val mobwrite = """U:fraser
                     |F:34:file1.tex
                     |d:41:=200	-7	+Hello	=100
                     |U:fraser
                     |F:53:file2.tex
                     |d:90:=123	+Fool	=296
                     |""".stripMargin
    val paperId = "paper_id"

    val session =
      SyncSession(
        "fraser",
        "paper_id",
        List(
          SyncCommand(
            "file1.tex",
            34,
            Delta(
              41,
              List(Equality(200), Delete(7), Add("Hello"), Equality(100)),
              false
            )
          ),
          SyncCommand(
            "file2.tex",
            53,
            Delta(
              90,
              List(Equality(123), Add("Fool"), Equality(296)),
              false
            )
          )
        )
      )

    val (convertedId, converted) = bluelatex2mobwrite(session)

    convertedId should be(paperId)
    converted should be(mobwrite)

  }

  "several sessions" should "be correctly transformed to blue-sync protocol" in {
    val mobwrite = """U:fraser
                     |F:12:file1
                     |M:{"toto": true, "gnieh": 23, "plop": {"toto": "gruik" }}
                     |F:13:file2
                     |n:file
                     |U:toto
                     |n:plop
                     |""".stripMargin

    val paperId = "paper_id"

    val sessions = List(
      SyncSession(
        "fraser",
        paperId,
        List(
          Message("fraser", ("toto" -> true) ~ ("gnieh" -> 23) ~ ("plop" -> ("toto" -> "gruik")), true, Some("file1")),
          SyncCommand("file", -1, Nullify)
        )
      ),
      SyncSession(
        "toto",
        paperId,
        List(
          SyncCommand("plop", -1, Nullify)
        )
      )
    )

    val converted = mobwrite2bluelatex(paperId, mobwrite)

    converted should be(sessions)

  }

  it should "be correctly transformed to mobwrite protocol" in {
    val mobwrite = """U:fraser
                     |F:file1
                     |M:{"toto":true,"gnieh":23,"plop":{"toto":"gruik"}}
                     |U:fraser
                     |N:file
                     |U:toto
                     |M:{"toto":true}
                     |U:toto
                     |N:plop
                     |""".stripMargin

    val paperId = "paper_id"

    val session1 = SyncSession(
      "fraser",
      paperId,
      List(
        Message("fraser", ("toto" -> true) ~ ("gnieh" -> 23) ~ ("plop" -> ("toto" -> "gruik")), true, Some("file1")),
        SyncCommand("file", -1, Nullify)
      )
    )

    val session2 = SyncSession(
      "toto",
      paperId,
      List(
        Message("toto", ("toto" -> true), true, None),
        SyncCommand("plop", -1, Nullify)
      )
    )

    val (convertedId1, converted1) = bluelatex2mobwrite(session1)
    val (convertedId2, converted2) = bluelatex2mobwrite(session2)

    convertedId1 should be(paperId)
    convertedId2 should be(paperId)
    (converted1 + converted2) should be(mobwrite)

  }

  "an empty raw command" should "be correctly translated" in {
    val mobwrite = """U:fraser
                     |F:file1
                     |r:1:
                     |""".stripMargin

    val paperId = "paper_id"

    val session = SyncSession(
      "fraser",
      paperId,
      List(
        SyncCommand("file1", -1, Raw(1, "", false))
      )
    )

    val converted = mobwrite2bluelatex(paperId, mobwrite)

    converted should be(List(session))

  }

}

