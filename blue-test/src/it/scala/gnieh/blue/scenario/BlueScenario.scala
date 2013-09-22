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
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.ning.http.client.{
  RequestBuilder,
  Response
}

import gnieh.diffson._

import net.liftweb.json._

/** Extend this class to implement a test scenario for a feature of \BlueLaTeX.
 *  It provides the environment that allows you to interact with the server and
 *  check that it runs ok.
 *
 *  @author Lucas Satabin
 */
abstract class BlueScenario extends FeatureSpec with GivenWhenThen with ShouldMatchers with BeforeAndAfterAll {

  object mailbox extends Mailbox

  override def beforeAll() {
    mailbox.start()
  }

  override def afterAll() {
    mailbox.stop()
  }

  type AsyncResult[T] = Future[Either[(Int, ErrorResponse), T]]

  val PasswordResetRegex = "(?s).*http://localhost:8080/reset\\.html\\?user=(.+)\\&token=(\\S+).*".r

  case class BlueErrorException(status: Int, error: ErrorResponse) extends Exception {
    override def toString  = s"error: $status, message: $error"
  }

  implicit val formats = DefaultFormats + JsonPatchSerializer

  private def request(path: List[String]) =
    path.foldLeft(:/("localhost", 8080)) { (acc, p) => acc / p }

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

  private def synced[T](result: AsyncResult[T]): T = Await.result(result, Duration.Inf) match {
    case Right(t) => t
    case Left((code, error)) =>
      throw BlueErrorException(code, error)
  }

  private def http(request: RequestBuilder): AsyncResult[JValue] =
    Http(request > handleResponse _)

  private def handleResponse(response: Response): Either[(Int, ErrorResponse), JValue] = {
    val json = JsonParser.parse(as.String(response))
    val code = response.getStatusCode
    if (code / 100 != 2) {
      // something went wrong...
      val error = json.extract[ErrorResponse]
      Left((code, error))
    } else {
      Right(json)
    }
  }

  /** Posts some data to the given path */
  def postData[T: Manifest](path: List[String], data: Any, parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()): T =
    synced(http(request(path) <<? parameters <:< headers << serialize(data))).extract[T]

  /** Posts some data as request parameters */
  def post[T: Manifest](path: List[String], parameters: Map[String, String], headers: Map[String, String] = Map()): T =
    synced(http(request(path) << parameters <:< headers)).extract[T]

  /** Gets some resource */
  def get[T: Manifest](path: List[String], parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()): T =
    synced(http(request(path) <<? parameters <:< headers)).extract[T]

  /** Patches some resource */
  def patch[T: Manifest](path: List[String], patch: JsonPatch, parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()): T =
    synced(http((request(path) <<? parameters <:< headers << serialize(patch)).PATCH)).extract[T]

  /** Deletes some resource */
  def delete[T: Manifest](path: List[String], parameters: Map[String, String] = Map(), headers: Map[String, String] = Map()): T =
    synced(http((request(path) <<? parameters <:< headers).DELETE)).extract[T]

}
