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
package core
package impl
package paper

import http._
import couch._
import common._
import permission._

import java.util.{
  Date,
  UUID
}
import java.io.{
  File,
  FileWriter
}

import tiscaf._

import com.typesafe.config.Config

import org.osgi.framework.BundleContext

import scala.collection.JavaConverters._

import resource._

import scala.sys.process._

import scala.util.{
  Try,
  Success
}

import gnieh.sohva.control.CouchClient

/** Create a new paper. The currently authenticated user is added as author of this paper
 *
 *  @author Lucas Satabin
 */
class CreatePaperLet(
  val couch: CouchClient,
  config: Config,
  context: BundleContext,
  templates: Templates,
  logger: Logger)
    extends SyncBlueLet(config, logger) with SyncAuthenticatedLet {

  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Try[Any] =
    (talk.req.param("paper_name"), talk.req.param("paper_title")) match {
      case (Some(name), Some(title)) =>

        val template = talk.req.param("template").getOrElse("article")

        val visibility = talk.req.param("visibility").getOrElse("private")

        val configuration = new PaperConfiguration(config)

        val manager = entityManager("blue_papers")

        val newId = s"x${UUID.randomUUID.getMostSignificantBits.toHexString}"

        for {
          // create the paper into the database
          () <- manager.create(newId, None)
          // add the core component which contains the type, the title
          paper <- manager.saveComponent(newId, Paper(s"$newId:core", name, new Date))
          // add the permissions component to set the creator as author
          roles <- manager.saveComponent(newId, PaperRole(s"$newId:roles", UsersGroups(Set(user.name), Set()), UsersGroups(Set(), Set()),
            UsersGroups(Set(), Set())))
          visibility <- manager.saveComponent(newId, phase(newId, visibility))
          user <- entityManager("blue_users").getComponent[User](s"org.couchdb.user:${user.name}")
        } yield {
          if(configuration.paperDir(newId).mkdirs) {

            // if the template is not one of the standard styles,
            // then there should be an associated .sty file to be copied in `resources'
            // directory
            val templateName = template match {
              case "article" | "book" | "report" =>
                // built-in template, ignore it
                "generic"
              case "beamer" =>
                "beamer"
              case cls if configuration.cls(cls).exists =>
                // copy the file to the working directory
                logDebug(s"Copying class ${configuration.cls(cls)} to paper directory ${configuration.paperDir(newId)}")
                (configuration.cls(cls) #> new File(configuration.paperDir(newId), cls + ".cls")) !
                  CreationProcessLogger
                cls
              case cls =>
                // just log that the template was not found. It can however be uploaded later by the user
                logDebug(s"Class $cls was not found, the user will have to upload it later")
                "generic"
            }

            // write the template to the newly created paper
            for(fw <- managed(new FileWriter(configuration.paperFile(newId)))) {
              fw.write(
                templates.layout(
                  s"$templateName.tex",
                  "class" -> template,
                  "title" -> title,
                  "id" -> newId,
                  "author" -> user.map(_.fullName).getOrElse("Your Name"),
                  "email" -> user.map(_.email).getOrElse("your@email.com"),
                  "affiliation" -> user.flatMap(_.affiliation).getOrElse("Institute")
                )
              )
            }

            // create empty bibfile
            configuration.bibFile(newId).createNewFile

            import OsgiUtils._

              // notifiy creation hooks
              for(hook <- context.getAll[PaperCreated])
                Try(hook.afterCreate(newId, manager)) recover {
                  case e => logError("Error in post paper creation hook", e)
                }
              talk.setStatus(HStatus.Created).writeJson(newId)

          } else {
            logError(s"Unable to create the paper directory: ${configuration.paperDir(newId)}")
            talk
              .setStatus(HStatus.InternalServerError)
              .writeJson(ErrorResponse("cannot_create_paper", "Something went wrong on the server side"))
          }
        }

      case (_, _) =>
        // missing parameter
        Success(
          talk
            .setStatus(HStatus.BadRequest)
            .writeJson(ErrorResponse("cannot_create_paper", "Some parameters are missing")))

    }

  private def phase(id: String, visibility: String): PaperPhase = {
    val permissions =
      if(config.hasPath(f"blue.permissions.$visibility-defaults"))
        config.getConfig(f"blue.permissions.$visibility-defaults")
      else
        config.getConfig("blue.permissions.private-defaults")
    val permissions1 = Map[String,List[Permission]](
      Author.toString -> permissions.getStringList("author").asScala.map(name => Permission(name)).toList,
      Reviewer.toString -> permissions.getStringList("reviewer").asScala.map(name => Permission(name)).toList,
      Guest.toString -> permissions.getStringList("guest").asScala.map(name => Permission(name)).toList,
      Other.toString -> permissions.getStringList("other").asScala.map(name => Permission(name)).toList,
      Anonymous.toString -> permissions.getStringList("anonymous").asScala.map(name => Permission(name)).toList
    )
    PaperPhase(f"$id:phase", "writing", permissions1, Nil)
  }

  object CreationProcessLogger extends ProcessLogger {
    def out(s: => String) =
      logInfo(s)
    def err(s: => String) =
      logError(s)
    def buffer[T](f: => T) = f
  }

}
