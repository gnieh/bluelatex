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
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.ning.http.client.{
  RequestBuilder,
  Response,
  Cookie
}

import scala.collection.JavaConverters.{
  mapAsScalaMapConverter,
  asScalaBufferConverter
}

import gnieh.diffson._

import net.liftweb.json._

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

  lazy val couch = {
    val sess = new CouchClient(port = couchPort).startSession
    sess.login(admin, password)
    sess
  }

  object mailbox extends Mailbox

  override def beforeAll(config: ConfigMap) {
    assume(config.isDefinedAt("couchPort"), "couchPort must be provided")
    assume(config.isDefinedAt("admin"), "admin must be provided")
    assume(config.isDefinedAt("password"), "password must be provided")
    try {
      couchPort = config.getRequired[String]("couchPort").toInt
      admin = config.getRequired[String]("admin")
      password = config.getRequired[String]("password")
    } catch {
      case e: Exception =>
        e.printStackTrace
    }
    mailbox.start()
    // ensure the databases exist
    couch.database("blue_users").create
    couch.database("blue_papers").create
  }

  override def afterAll(config: ConfigMap) {
    mailbox.stop()
    // cleanup databases from all non design documents
    val usersDb = couch.database("blue_users")
    // filter out design documents
    val userDocs = usersDb._all_docs.filter(!_.startsWith("_design/"))
    // and delete them
    usersDb.deleteDocs(userDocs)
    val papersDb = couch.database("blue_papers")
    // filter out design documents
    val paperDocs = papersDb._all_docs.filter(!_.startsWith("_design/"))
    papersDb.deleteDocs(paperDocs)
  }

  type AsyncResult[T] = Future[Result[T]]

  type Result[T] = Either[(Int, ErrorResponse), (T, Map[String, List[String]])]

  val PasswordResetRegex = s"(?s).*http://localhost:$bluePort/reset\\.html\\?user=(.+)\\&token=(\\S+).*".r

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

  private def http[T: Manifest](request: RequestBuilder): AsyncResult[T] = cookie match {
    case Some(cookie) =>
      Http(request.addCookie(cookie) > handleResponse[T] _)
    case None =>
      Http(request > handleResponse[T] _)
  }

  private def handleResponse[T: Manifest](response: Response): Result[T] = {
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

  /** Posts some data to the given path */
  def postData[T: Manifest](path: List[String], data: Any, parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()) =
    synced(http[T](request(path) <<? parameters <:< headers << serialize(data)))

  /** Posts some data as request parameters */
  def post[T: Manifest](path: List[String], parameters: Map[String, String], headers: Map[String, String] = Map()) =
    synced(http[T](request(path) << parameters <:< headers))

  /** Gets some resource */
  def get[T: Manifest](path: List[String], parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()) =
    synced(http[T](request(path) <<? parameters <:< headers))

  /** Patches some resource */
  def patch[T: Manifest](path: List[String],
                         patch: JsonPatch,
                         revision: String,
                         parameters: Map[String, String] = Map(),
                         headers: Map[String, String] = Map()) =
                           synced(http[T]((request(path) <<? parameters <:< Map("Content-Type" -> "application/json", "If-Match" -> revision) <:< headers << serialize(patch)).PATCH))

  /** Deletes some resource */
  def delete[T: Manifest](path: List[String], parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()) =
    synced(http[T]((request(path) <<? parameters <:< headers).DELETE))

}
