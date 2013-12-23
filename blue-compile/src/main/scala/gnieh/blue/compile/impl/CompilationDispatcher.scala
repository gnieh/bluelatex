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
package compile
package impl

import scala.language.postfixOps

import couch.{
  Paper,
  User
}

import gnieh.sohva.control.CouchClient

import akka.actor.{
  Actor,
  Props,
  ReceiveTimeout
}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

import java.io.{
  File,
  FilenameFilter
}

import scala.sys.process._

import com.typesafe.config.Config

import org.osgi.framework.BundleContext

/** The compilation system actor is responsible for managing
 *  the compilation actor for each paper
 *
 *  @author Lucas Satabin
 */
class CompilationDispatcher(bndContext: BundleContext, config: Config) extends ResourceDispatcher {

  val configuration = new CompileConfiguration(config)

  def props(username: String, paperId: String) =
    Props(new CompileActor(bndContext, configuration, paperId))

}

/** This actor handles compilation of a paper.
 *  An instance of this actor is not eternally resident, if no
 *  compilation message was received within a session timeout,
 *  this actor is destroyed.
 *
 *  @author Lucas Satabin
 *
 */
class CompileActor(bndContext: BundleContext, configuration: CompileConfiguration, paperId: String) extends Actor with Logging {

  val extractor = new TeXInfoExtractors(configuration)

  // send a timeout message when no compilation message was received
  // within configured session timeout
  context.setReceiveTimeout(configuration.config.getInt("blue.session.timeout") minutes)

  def receive = {
    case PdfLatex(id, density, timeout, user) if id == paperId =>
      if (logger.isDebugEnabled)
        logger.debug("Compilation process starts for paper " + paperId + " with pdflatex")
      // compile with pdflatex and return execution code to sender
      sender ! compile(pdflatex(timeout), density, user)(timeout)

      if (logger.isDebugEnabled)
        logger.debug("Compilation process done for paper " + paperId + " with pdflatex")

    case Latex(id, density, timeout, user) if id == paperId =>
      if (logger.isDebugEnabled)
        logger.debug("Compilation process starts for paper " + paperId + " with latex")
      // compile with latex
      val ret = compile(latex(timeout), density, user)(timeout)
      // then convert to pdf
      if (ret == 0)
        dvipdfmx(timeout)
      // return execution code to sender
      sender ! ret

      if (logger.isDebugEnabled)
        logger.debug("Compilation process done for paper " + paperId + " with latex")
    case ReceiveTimeout =>
      if (logger.isDebugEnabled)
        logger.debug("Stop compile actor for paper " + paperId)
      // stop this actor
      context.stop(self)
  }

  def compile(compiler: => Int, density: Int, user: User)(implicit timeout: Timeout) = {

    import common.OsgiUtils._

    // first persists files to the persistent storage
    for(sync <- bndContext.get[SynchroServer]) {
      sync.persist(paperId)
    }

    try {
      for(couch <- bndContext.get[CouchClient]) {
        // save title and class in couch
        val database = couch.database("blue_papers")
        val title = extractor.texTitle(paperId)
        val clazz = extractor.documentClass(paperId)
        database.getDocById[Paper](paperId) map {
          case Some(paper) =>
            val authors = paper.authors + user._id
            if (paper.title != title || paper.cls != clazz || paper.authors != authors)
              database.saveDoc(paper.copy(title = title, authors = authors, cls = clazz))
          case None =>
            // it does not exist yet, create it
            // (good for old papers with no couch support)
            database.saveDoc(Paper(paperId, title, Set(user._id), Set(), clazz))
        } recover {
          case e: Exception =>
            logger.error("Unable to save paper settings in database for paper "
              + paperId, e)
        }
      }

      if (!configuration.buildDir(paperId).exists) {
        if (logger.isDebugEnabled)
          logger.debug("Creating build directory for paper " + paperId)
        configuration.buildDir(paperId).mkdir
      }

      if (logger.isDebugEnabled)
        logger.debug("Compile paper " + paperId)
      // compile the paper
      val compilerRet = compiler

      if (compilerRet != 0) {
        logger.warn("compiling .tex file returned " + compilerRet)
      }

      if (logger.isDebugEnabled)
        logger.debug("Run bibtex on " + paperId)
      // run bibtex
      val ret = bibtex

      if (ret != 0) {
        logger.warn("bibtex returned " + ret)
      }
      // XXX as long as we don't have a dedicated and efficient compilation server
      // the user will have to compile many times by himself to have citations and
      // references correctly compiled...
      //      compiler()
      //      compiler()

      import common.FileProcessing._

      // cleanup generated png files
      for(file <- configuration.buildDir(paperId).filter(f => f.extension == ".png")) {
        if (logger.isDebugEnabled)
          logger.debug("Cleaning page " + file)
        file.delete
      }


      compilerRet
    } catch {
      case e: Exception =>
        logger.error("Something went wrong while compiling", e)
        // send error to sender actor
        sender ! akka.actor.Status.Failure(e)
        throw e
    }
  }

  // ========= helper methods =========

  private def send(command: String, workingDir: File, env: List[(String, String)] = List())(implicit timeout: Timeout) =
    Await.result(context.actorSelection("/user/system-commands") ? SystemCommand(command, workingDir, env, timeout) mapTo manifest[Int],
      timeout.duration)

  val buildDir = configuration.buildDir(paperId).getCanonicalPath
  val paperFile = configuration.paperFile(paperId).getCanonicalPath

  def pdflatex(implicit timeout: Timeout) =
    send(s"pdflatex -interaction nonstopmode -output-directory $buildDir $paperFile",
      configuration.paperDir(paperId)) //,
      //List("TEXINPUT" -> ".:tex/:resources/:$TEXINPUTS"))

  def latex(implicit timeout: Timeout) =
    send(s"latex -interaction nonstopmode -output-directory $buildDir $paperFile",
      configuration.paperDir(paperId)) //,
      //List("TEXINPUT" -> ".:tex/:resources/:$TEXINPUTS"))

  def bibtex(implicit timeout: Timeout) = {
    // XXX this sucks! we need to copy the bib files to the build directory because bibtex
    // cannot handle compilation in a different directory correctly
    // technology from the 80's has limitations...
    // http://tex.stackexchange.com/questions/12686/how-do-i-run-bibtex-after-using-the-output-directory-flag-with-pdflatex-when-f
    import common.FileProcessing._
    for(file <- configuration.paperDir(paperId).filter(_.extension == ".bib")) {
      val destfile = new File(configuration.buildDir(paperId), file.getName)
      (file #> destfile)!
    }

    send(s"bibtex $paperId", configuration.buildDir(paperId))
  }

  def dvipdfmx(implicit timeout: Timeout) =
    send(s"dvipdfmx $paperId.dvi", configuration.buildDir(paperId))

}

