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

import tiscaf._

import gnieh.diffson._


import couch.Paper

import common._

import com.typesafe.config.Config

import net.liftweb.json._

import scala.io.Source

import scala.concurrent._

import scala.util.{
  Try,
  Success,
  Failure
}

import scala.language.higherKinds
import scala.language.implicitConversions

object BlueLet {

  /** The formats to (de)serialize json objects. You may override it if you need specific serializers */
  implicit def formats = DefaultFormats + JsonPatchSerializer

  /** Enriches the standard tiscaf `HTalk` object with methods that are useful in \BlueLaTeX
   *
   *  @author Lucas Satabin
   */
  implicit class RichTalk(val talk: HTalk) extends AnyVal {

    def serialize(obj: Any): JValue = obj match {
      case i: Int => JInt(i)
      case i: BigInt => JInt(i)
      case l: Long => JInt(l)
      case d: Double => JDouble(d)
      case f: Float => JDouble(f)
      case d: BigDecimal => JDouble(d.doubleValue)
      case b: Boolean => JBool(b)
      case s: String => JString(s)
      case _ => Extraction.decompose(obj) remove {
        // drop couchdb specific fields
        case JField("_id" | "_rev", _) => true
        case _                         => false
      }
    }

    /** Serializes the value to its json representation and writes the response to the client,
     *  corrrectly setting the result type and length */
    def writeJson(json: Any): Unit = {
      val response = pretty(render(serialize(json)))
      val array = response.getBytes(talk.encoding)
      talk
        .setContentType(s"${HMime.json};charset=${talk.encoding}")
        .setContentLength(array.length)
        .write(array)
    }

    /** Serializes the value to its json representation and writes the response to the client,
     *  corrrectly setting the result type and length and the revision of the modified resource
     *  in the `ETag` field */
    def writeJson(json: Any, rev: String): Unit = {
      talk.setHeader("ETag", rev)
      writeJson(json)
    }

    /** Reads the content of the body as a Json value and extracts it as `T` */
    def readJson[T: Manifest]: Option[T] =
      (for {
        tpe <- talk.req.header("content-type")
        if tpe.startsWith(HMime.json)
        octets <- talk.req.octets
      } yield {
        // try to infer the encoding from the Content-Type header
        // otherwise it is ISO-8859-1
        val charset = talk.req.contentEncoding
        val json = JsonParser.parse(Source.fromBytes(octets, charset).mkString)
        json.extractOpt[T]
      }).flatten

  }

}

/** A `HLet` that is enriched with all utilities that are useful in \BlueLaTeX.
 *
 *  @author Lucas Satabin
 */
sealed abstract class BlueLet[Ret[_]](val config: Config, val logger: Logger) extends HLet with CouchSupport with Logging {

  implicit val formats = BlueLet.formats

  lazy val configuration = new PaperConfiguration(config)

  @inline
  implicit def talk2rich(talk: HTalk): BlueLet.RichTalk =
    new BlueLet.RichTalk(talk)

  def act(talk: HTalk): Ret[Any]

  protected def try2future[T](t: Try[T]): Future[T] =
    t match {
      case Success(v) => Future.successful(v)
      case Failure(t) => Future.failed(t)
    }

}

/** A `HLet` used to perform synchronous tasks
 *
 *  @author Lucas Satabin
 */
abstract class SyncBlueLet(config: Config, logger: Logger) extends BlueLet[Try](config, logger) {

  final override def aact(talk: HTalk) =
    try2future(act(talk) recoverWith {
      case t =>
        logError("Something went wrong", t)
        Try(
          talk.writeJson(
            ErrorResponse(
              "unexpected_error",
              "Something went really wrong. If the problem persists contact an administrator")))
    })

}

/** A `HLet` used to perform asynchronous tasks
 *
 *  @author Lucas Satabin
 */
abstract class AsyncBlueLet(config: Config, logger: Logger) extends BlueLet[Future](config, logger) {

  /** Override this to enable your custom execution context if needed */
  implicit val executionContext = ExecutionContext.Implicits.global

  @inline
  final override def aact(talk: HTalk) =
    act(talk) recoverWith {
      case t =>
        logError("Something went wrong", t)
        Future(
          talk.writeJson(
            ErrorResponse(
              "unexpected_error",
              "Something went really wrong. If the problem persists contact an administrator")))
    }

}

/** Mix this trait in to add support for authentication for this action
 *
 *  @author Lucas Satabin
 */
trait SyncAuthenticatedLet {
  this: SyncBlueLet =>

  override def act(talk: HTalk) =
    currentUser(talk) flatMap {
      case Some(user) =>
        authenticatedAct(user)(talk)
      case None =>
        unauthenticatedAct(talk)
    }

  /** The action to take when the user is authenticated */
  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Try[Any]

  /** The action to take when the user is not authenticated.
   *  By default sends an error object with code "Unauthorized"
   */
  def unauthenticatedAct(implicit talk: HTalk): Try[Any] =
    Success(talk
      .setStatus(HStatus.Unauthorized)
      .writeJson(ErrorResponse("unauthorized", "This action is only permitted to authenticated people")))

}

/** Mix this trait in to add support for authentication for this action
 *
 *  @author Lucas Satabin
 */
trait AsyncAuthenticatedLet {
  this: AsyncBlueLet =>

  override def act(talk: HTalk) =
    try2future(currentUser(talk)) flatMap {
      case Some(user) =>
        authenticatedAct(user)(talk)
      case None =>
        unauthenticatedAct(talk)
    }

  /** The action to take when the user is authenticated */
  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Future[Any]

  /** The action to take when the user is not authenticated.
   *  By default sends an error object with code "Unauthorized"
   */
  def unauthenticatedAct(implicit talk: HTalk): Future[Any] =
    Future.successful(talk
      .setStatus(HStatus.Unauthorized)
      .writeJson(ErrorResponse("unauthorized", "This action is only permitted to authenticated people")))

}

/** Extends this class if you need to treat differently authors, reviewers or other users
 *  for a given paper.
 *
 *  @author Lucas Satabin
 */
abstract class SyncRoleLet(val paperId: String, config: Config, logger: Logger) extends SyncBlueLet(config, logger) with SyncAuthenticatedLet {

  private def roles(implicit talk: HTalk): Try[Map[String, PaperRole]] =
    couchSession.database(couchConfig.database("blue_papers")).getDocById[Paper](paperId) map {
      case Some(Paper(_, _, authors, reviewers, _, _, _, _, _, _)) =>
        (authors.map(id => (id, Author)) ++
          reviewers.map(id => (id, Reviewer))).toMap.withDefaultValue(Other)
      case None =>
        Map().withDefaultValue(Other)
    }

  final def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Try[Any] =
    roles(talk) flatMap { m =>
      roleAct(user, m(user.name))
    }

  /** Implement this method that can behave differently depending on the user
   *  role for the current paper.
   *  It is only called when the user is authenticated
   */
  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Try[Any]

}

/** Extends this class if you need to treat differently authors, reviewers or other users
 *  for a given paper.
 *
 *  @author Lucas Satabin
 */
abstract class AsyncRoleLet(val paperId: String, config: Config, logger: Logger) extends AsyncBlueLet(config, logger) with AsyncAuthenticatedLet {

  private def roles(implicit talk: HTalk): Try[Map[String, PaperRole]] =
    couchSession.database(couchConfig.database("blue_papers")).getDocById[Paper](paperId) map {
      case Some(Paper(_, _, authors, reviewers, _, _, _, _, _, _)) =>
        (authors.map(id => (id, Author)) ++
          reviewers.map(id => (id, Reviewer))).toMap.withDefaultValue(Other)
      case None =>
        Map().withDefaultValue(Other)
    }

  final def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Future[Any] =
    roles(talk) match {
      case Success(m) => roleAct(user, m(user.name))
      case Failure(t) => Future.failed(t)
    }

  /** Implement this method that can behave differently depending on the user
   *  role for the current paper.
   *  It is only called when the user is authenticated
   */
  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Future[Any]

}

