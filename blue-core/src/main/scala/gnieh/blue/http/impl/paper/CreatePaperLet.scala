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
package http
package impl
package paper

import couch._

import java.util.UUID
import java.io.{
  File,
  FileWriter
}

import tiscaf._

import com.typesafe.config.Config

import resource._

import scala.sys.process._

/** Create a new paper. The currently authenticated user is added as author of this paper
 *
 *  @author Lucas Satabin
 */
class CreatePaperLet(config: Config, templates: Templates) extends AuthenticatedLet(config) {

  def authenticatedAct(user: User)(implicit talk: HTalk): Unit = {

    // new paper identifier
    val newId = "w" + UUID.randomUUID.getMostSignificantBits.toHexString

    val title = talk.req.param("paper_title").getOrElse("New Paper")
    val template = talk.req.param("template").getOrElse("article")

    val configuration = new PaperConfiguration(config)

    // if the template is not one of the standard styles,
    // then there should be an associated .sty file to be copied in `resources'
    // directory
    val clazz = template match {
      case "article" | "book" | "report"        => None
      case cls if configuration.cls(cls).exists => Some(cls)
      case cls =>
        // TODO just log and fallback to article
        None
    }

    configuration.paperDir(newId).mkdirs

    // write the template to the newly created paper
    for(fw <- managed(new FileWriter(configuration.paperFile(newId)))) {
      fw.write(templates.layout(s"$template.tex", "title" -> title, "id" -> newId))
    }

    // copy the style file if necessary
    clazz match {
      case Some(cls) =>
        // copy the file to the working directory only
        // it cannot be deleted by the user
        (configuration.cls(cls) #> new File(configuration.paperDir(newId), cls + ".cls")) !
          CreationProcessLogger
      case _ => // do nothing
    }

    // create empty bibfile
    configuration.bibFile(newId).createNewFile

    // create the paper database
    for(db <- database("blue_papers"))
      db.saveDoc(Paper(newId, title, Set(user._id), Set(), template))

  }

  object CreationProcessLogger extends ProcessLogger {
    def out(s: => String) = ???
    def err(s: => String) = ???
    def buffer[T](f: => T) = f
  }

}
