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
package util

import javax.mail._
import javax.mail.internet._

import java.util.{
  Date,
  Properties
}

/** Encapsulates the logic to send emails from the \Blue platform
 *
 * @author Lucas Satabin
 */
class MailAgent(configuration: BlueConfiguration) {

  /** Returns the list of all email addresses */
  private def retrieveEmail(username: String): Option[String] =
    configuration.couch.database("blue_users")
      .design("lists")
      .view[String, String, Nothing]("emails")
      .query(key = Some("org.couchdb.user:" + username))
      .rows
      .headOption
      .map(_.value)

  def send(username: String, subject: String, text: String) =
    for(to <- retrieveEmail(username)) {
      val from = new InternetAddress(configuration.emailConf.getProperty("mail.from"))

      val message = newMessage
      message.setFrom(from)
      message.setRecipient(Message.RecipientType.TO, new InternetAddress(to))
      message.setSubject(subject)
      message.setText(text)

      Transport.send(message)
    }

  private def newMessage = {
    val props = configuration.emailConf
    val session = Session.getDefaultInstance(props, new SmtpAuthenticator(props))
    new MimeMessage(session)
  }

}

class SmtpAuthenticator(conf: Properties) extends Authenticator {
  override def getPasswordAuthentication = {
    val user = Option(conf.getProperty("mail.smtp.user"))
    val pwd = Option(conf.getProperty("mail.smtp.password"))
    (user, pwd) match {
      case (Some(user), Some(pwd)) => new PasswordAuthentication(user, pwd)
      case _ => null
    }
  }
}
