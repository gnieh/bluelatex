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
package mobwrite

import sync._

import resource._

import java.net.Socket
import java.io.BufferedInputStream

import scala.io.Source

import com.typesafe.config.Config

/** A synchronization server that simply delegates the work
 *  to a standalone mobwrite daemon
 *
 *  @author Lucas Satabin
 */
class MobwriteServer(configuration: Config) extends SynchroServer {

  val url = configuration.getString("mobwrite.url")
  val port = configuration.getInt("mobwrite.port")

  def session(data: String): String = {
    (for(socket <- managed(new Socket(url, port))) yield {
      // Write data to daemon
      val outputStream = socket.getOutputStream
      outputStream.write(data.getBytes)
      outputStream.flush
      // Read the response from python and copy it to out
      val source = Source.fromInputStream(socket.getInputStream)

      source.foldLeft(new StringBuilder) { (res, current) =>
        res += current
      }.toString
    }).opt.getOrElse("")
  }

  def persist(paperId: String): Unit = {
  }

}
