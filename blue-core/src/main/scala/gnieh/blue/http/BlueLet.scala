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

import gnieh.sohva.UserInfo

import couch.Paper

import com.typesafe.config.Config

import net.liftweb.json._

import scala.io.Source

/** All modules in \BlueLaTeX should implement `BlueLet` or one of its derivatives
 *
 *  @author Lucas Satabin
 */
abstract class BlueLet(val config: Config) extends HSimpleLet with CouchSupport {

  import scala.language.implicitConversions

  /** The formats to (de)serialize json objects. You may override it if you need specific serializers */
  implicit def formats = DefaultFormats + JsonPatchSerializer

  /** Enriches the standard tiscaf `HTalk` object with methods that are useful in \BlueLaTeX
   *
   *  @author Lucas Satabin
   */
  class RichTalk(val talk: HTalk) {

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
    def writeJson(json: Any): HTalk = {
      val response = pretty(render(serialize(json)))
      talk
        .setContentType(HMime.json)
        .setContentLength(response.length)
        .write(response)
    }

    /** Serializes the value to its json representation and writes the response to the client,
     *  corrrectly setting the result type and length and the revision of the modified resource
     *  in the `ETag` field */
    def writeJson(json: Any, rev: String): HTalk =
      writeJson(json).setHeader("ETag", rev)

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

  @inline
  implicit def talk2rich(talk: HTalk): RichTalk =
    new RichTalk(talk)

}

/** Extend this class if you need to treat differently authenticated and unauthenticated
 *  users.
 *
 *  @author Lucas Satabin
 */
abstract class AuthenticatedLet(config: Config) extends BlueLet(config) {

  final def act(talk: HTalk): Unit =
    currentUser(talk) match {
      case Some(user) =>
        authenticatedAct(user)(talk)
      case None =>
        unauthenticatedAct(talk)
    }

  /** The action to take when the user is authenticated */
  def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Unit

  /** The action to take when the user is not authenticated.
   *  By default sends an error object with code "Unauthorized"
   */
  def unauthenticatedAct(implicit talk: HTalk): Unit = {
    talk
      .setStatus(HStatus.Unauthorized)
      .writeJson(ErrorResponse("unauthorized", "This action is only permitted to authenticated people"))
  }

}

/** Extends this class if you need to treat differently authors, reviewers or other users
 *  for a given paper.
 *
 *  @author Lucas Satabin
 */
abstract class RoleLet(val paperId: String, config: Config) extends AuthenticatedLet(config) {

  lazy val configuration = new PaperConfiguration(config)

  private def roles(implicit talk: HTalk): Map[String, PaperRole] =
    (for {
      Paper(_, _, authors, reviewers, _, _, _, _, _) <- couchSession.database(
        couchConfig.database("blue_papers")).getDocById[Paper](paperId)
    } yield {
      (authors.map(id => (id, Author)) ++
        reviewers.map(id => (id, Reviewer))).toMap.withDefaultValue(Other)
    }).getOrElse(Map().withDefaultValue(Other))

  final def authenticatedAct(user: UserInfo)(implicit talk: HTalk): Unit =
    roleAct(user, roles(talk)(s"org.couchdb.user:${user.name}"))

  /** Implement this method that can behave differently depending on the user
   *  role for the current paper.
   *  It is only called when the user is authenticated
   */
  def roleAct(user: UserInfo, role: PaperRole)(implicit talk: HTalk): Unit

}
