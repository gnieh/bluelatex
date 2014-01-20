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

import scala.util.parsing.combinator._

/** A simple parser for Edit commands.
 * @author Lucas Satabin
 *
 */
object EditCommandParsers extends RegexParsers {

  def parseEdits(input: String): List[Edit] =
    parseAll(repsep(edit, '\t'), input) match {
      case Success(res, _) => res
      case _ => Nil
    }

  lazy val edit: Parser[Edit] =
    ("+" ~> data ^^ Add
      | "-" ~> number ^^ Delete
      | "=" ~> number ^^ Equality)

  private lazy val number: Parser[Int] =
    "[0-9]+".r ^^ (_.toInt)

  private lazy val data: Parser[String] =
    "([-A-Za-z0-9_.!~*'();/?:@&=+$,# ]|%[A-Fa-f0-9]{2})+".r

}
