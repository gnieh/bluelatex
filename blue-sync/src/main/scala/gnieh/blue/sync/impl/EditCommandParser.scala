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

  override def skipWhitespace = false

  def parseEdits(input: String): List[Edit] =
    parseAll(repsep(edit, '\t'), input) match {
      case Success(res, _) => res
      case f               => Nil
    }

  lazy val edit: Parser[Edit] =
    ("+" ~> data ^^ Add
      | "-" ~> number ^^ Delete
      | "=" ~> number ^^ Equality)

  private lazy val number: Parser[Int] =
    "[0-9]+".r ^^ (_.toInt)

  private lazy val data: Parser[String] =
   // Actual regex should be the following one:
   // "([-A-Za-z0-9_.!~*'();/?:@&=+$,# ]|%[A-Fa-f0-9]{2})+".r
   // However, because of Java RE engine, it produces a stackoverflow
   // when applied on a "more than a few characters" text.
   // So let's fall-back to a simplier regex for the moment.
   // Note that we do not validate user input anymore...
    "[^\t]+".r

}
