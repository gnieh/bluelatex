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
package scenario

import http.ErrorResponse

import org.scalatest._

import dispatch._

import gnieh.sohva.sync._
import gnieh.sohva.sync.entities.EntityManager

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.ning.http.client.Cookie

import scala.collection.JavaConverters.{
  mapAsScalaMapConverter,
  asScalaBufferConverter
}

import gnieh.diffson._

import net.liftweb.json._

import java.io.File

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._

/** Extend this class to implement a test scenario for a feature of \BlueLaTeX.
 *  It provides the environment that allows you to interact with the server and
 *  check that it runs ok.
 *
 *  @author Lucas Satabin
 */
abstract class BlueScenario extends FeatureSpec
                            with GivenWhenThen
                            with ShouldMatchers
                            with BeforeAndAfterAllConfigMap {

  val bluePort = 18080

  var couchPort = 5984

  var admin = "admin"

  var password = "admin"

  private var cookie: Option[Cookie] = None

  implicit val system = ActorSystem()

  implicit val timeout = Timeout(20.seconds)

  lazy val ccouch = new CouchClient(port = couchPort)

  lazy val couch = {
    val sess = ccouch.startCookieSession
    sess.login(admin, password)
    sess
  }

  lazy val paperManager = new EntityManager(couch.database("blue_papers"))
  lazy val userManager = new EntityManager(couch.database("blue_users"))

  object mailbox extends Mailbox

  override def beforeAll(config: ConfigMap): Unit =  try {
    super.beforeAll(config)
  } finally {
    try {
      assume(config.isDefinedAt("couchPort"), "couchPort must be provided")
      assume(config.isDefinedAt("admin"), "admin must be provided")
      assume(config.isDefinedAt("password"), "password must be provided")
      couchPort = config.getRequired[String]("couchPort").toInt
      admin = config.getRequired[String]("admin")
      password = config.getRequired[String]("password")
      mailbox.start()
      // ensure the databases exist
      couch.database("blue_users").create
      couch.database("blue_papers").create
    } catch {
      case e: Exception =>
        e.printStackTrace
    }
  }

  override def afterAll(config: ConfigMap): Unit = try {
    super.afterAll(config)
  } finally {
    mailbox.stop()
    // cleanup databases from all non design documents
    val usersDb = couch.database("blue_users")
    // filter out design documents
    val userDocs = usersDb._all_docs().filter(!_.startsWith("_design/"))
    // and delete them
    if(userDocs.nonEmpty)
      usersDb.deleteDocs(userDocs)
    val papersDb = couch.database("blue_papers")
    // filter out design documents
    val paperDocs = papersDb._all_docs().filter(!_.startsWith("_design/"))
    if(paperDocs.nonEmpty)
      papersDb.deleteDocs(paperDocs)
    ccouch.shutdown()
    system.shutdown()
  }

  type AsyncResult[T] = Future[Result[T]]

  type Result[T] = Either[(Int, ErrorResponse), (T, Map[String, List[String]])]

  val PasswordResetRegex = s"(?s).*http://localhost:$bluePort/web/index\\.html#/([^/]+)/reset/(\\S+).*".r

  case class BlueErrorException(status: Int, error: ErrorResponse) extends Exception {
    override def toString  = s"error: $status, message: $error"
  }

  implicit val formats = DefaultFormats + JsonPatchSerializer

  private def request(path: List[String]) =
    path.foldLeft(:/("localhost", bluePort)) { (acc, p) => acc / p }

  private def serialize(obj: Any): String = pretty(render(obj match {
    case i: Int => JInt(i)
    case i: BigInt => JInt(i)
    case l: Long => JInt(l)
    case d: Double => JDouble(d)
    case f: Float => JDouble(f)
    case d: BigDecimal => JDouble(d.doubleValue)
    case b: Boolean => JBool(b)
    case s: String => JString(s)
    case _ => Extraction.decompose(obj)
  }))

  private def synced[T](result: AsyncResult[T]): (T, Map[String, List[String]]) = Await.result(result, Duration.Inf) match {
    case Right(t) => t
    case Left((code, error)) =>
      throw BlueErrorException(code, error)
  }

  private def http[T: Manifest](request: Req): AsyncResult[T] = cookie match {
    case Some(cookie) =>
      Http(request.addCookie(cookie) > handleResponse[T] _)
    case None =>
      Http(request > handleResponse[T] _)
  }

  private def rawHttp(request: Req): AsyncResult[String] = cookie match {
    case Some(cookie) =>
      Http(request.addCookie(cookie) > handleRawResponse _)
    case None =>
      Http(request > handleRawResponse _)
  }

  private def handleResponse[T: Manifest](response: Res): Result[T] = {
    val str = as.String(response)
    val json = JsonParser.parse(str)
    val code = response.getStatusCode
    cookie = response.getCookies.asScala.headOption
    val headers = response.getHeaders.asScala.map { case (k, v) => (k, v.asScala.toList) }.toMap
    if (code / 100 != 2) {
      // something went wrong...
      val error = json.extract[ErrorResponse]
      Left((code, error))
    } else {
      Right(json.extract[T] -> headers)
    }
  }

  private def handleRawResponse(response: Res): Result[String] = {
    val str = as.String(response)
    val code = response.getStatusCode
    cookie = response.getCookies.asScala.headOption
    val headers = response.getHeaders.asScala.map { case (k, v) => (k, v.asScala.toList) }.toMap
    if (code / 100 != 2) {
      // something went wrong...
      val json = JsonParser.parse(str)
      val error = json.extract[ErrorResponse]
      Left((code, error))
    } else {
      Right(str -> headers)
    }
  }

  /** Posts some data to the given path */
  def postData[T: Manifest](path: List[String], data: Any, parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()) =
    synced(http[T](request(path) << parameters <:< headers << serialize(data)))

  /** Posts the content of the given file */
  def postFile[T: Manifest](path: List[String],
                            file: File,
                            mime: String,
                            parameters: Map[String, String] = Map(),
                            headers: Map[String, String] = Map()) =
    synced(http[T]((request(path) <<< file).POST <<? parameters <:< Map("Content-Type" -> mime) <:< headers))

  /** Posts some data as request parameters */
  def post[T: Manifest](path: List[String], parameters: Map[String, String], headers: Map[String, String] = Map()) =
    synced(http[T](request(path) << parameters <:< headers))

  /** Posts some data as form data */
  def postForm[T: Manifest](path: List[String], parameters: Map[String, String], headers: Map[String, String] = Map()) =
    synced(http[T](request(path) << parameters.map { case (k, v) => s"$k=$v" }.mkString("&") <:< Map("Content-Type" -> "application/x-www-form-urlencoded; charset=UTF-8") <:< headers))

  /** Gets some resource */
  def get[T: Manifest](path: List[String], parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()) =
    synced(http[T](request(path) <<? parameters <:< headers))

  /** Gets raw result */
  def getRaw(path: List[String], parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()) =
    synced(rawHttp(request(path) <<? parameters <:< headers))

  /** Patches some resource */
  def patch[T: Manifest](path: List[String],
                         patch: JsonPatch,
                         revision: String,
                         parameters: Map[String, String] = Map(),
                         headers: Map[String, String] = Map()) =
    synced(http[T]((request(path) <<? parameters <:< Map("Content-Type" -> "application/json", "If-Match" -> revision) <:< headers).PATCH << serialize(patch)))

  /** Deletes some resource */
  def delete[T: Manifest](path: List[String], parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()) =
    synced(http[T]((request(path) <<? parameters <:< headers).DELETE))

  /** Log the person in */
  def login(person: Person) =
    post[Boolean](List("session"), Map("username" -> person.username, "password" -> person.password))

}
