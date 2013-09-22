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

import org.subethamail.wiser._

import scala.collection.JavaConverters._

import scala.concurrent.stm._

/** The mailbox to read emails sent by the test \BlueLaTeX instance
 *
 *  @author Lucas Satabin
 */
class Mailbox {

  private var wiser: Option[Wiser] = None

  /** The port on which to listen, by default `2500` */
  var port: Int = 12525

  /** The hostname on which to listen, by default `localhost` */
  var hostname: String = "127.0.0.1"

  /** The timeout used to wait for received emails, by default, it is 10 seconds
   *  but you can modify it */
  var timeout: Long = 10000l

  /** Gets the last received email for the given email address
   *  and waits until timeout. If no email is received within the
   *  given delay, `None` is returned. If an email is found, it is dropped
   *  so that it cannot be returned twice. */
  def lastMailFor(address: String): Option[String] =
    atomic.withRetryTimeout(timeout) { implicit txn =>
      wiser.flatMap { w =>
        val message = w.getMessages.asScala.find(_.getEnvelopeReceiver == address)

        if(!message.isDefined)
          retry

        for(m <- message) yield {
          w.getMessages.asScala -= m
          m.toString
        }
      }
    }

  def start(): Unit = synchronized {
    if(wiser.isEmpty) {
      val w = new Wiser
      w.setPort(port)
      w.setHostname(hostname)
      wiser = Some(w)
      w.start
    }
  }

  def stop(): Unit = synchronized {
    wiser.foreach(_.stop)
  }

}

