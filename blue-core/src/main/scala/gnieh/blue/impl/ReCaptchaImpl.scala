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
package impl

import tiscaf._

import net.tanesha.recaptcha.{ ReCaptchaImpl, ReCaptchaResponse }

/** @author Lucas Satabin
 *
 */
class ReCaptchaUtilImpl(configuration: BlueConfiguration) extends ReCaptcha {

  val RECAPTCHA_RESPONSE_FIELD = "recaptcha_response_field"
  val RECAPTCHA_CHALLENGE_FIELD = "recaptcha_challenge_field"

  /** ReCaptcha is enabled if the private key is configured */
  def enabled_? =
    configuration.recaptchaPrivateKey.isDefined

  def verify(talk: HTalk) = if (enabled_?) {
    val challenge = talk.req.param(RECAPTCHA_CHALLENGE_FIELD)
    val response = talk.req.param(RECAPTCHA_RESPONSE_FIELD)

    (challenge, response) match {
      case (Some(c), Some(r)) =>
        val reCaptcha = new ReCaptchaImpl

        // no exception will be thrown because with checked that the private key
        // is defined
        reCaptcha.setPrivateKey(configuration.recaptchaPrivateKey.get)

        val reCaptchaResponse =
          reCaptcha.checkAnswer(talk.req.remoteIp, c, r)

        reCaptchaResponse.isValid
      case _ => false
    }
  } else {
    true
  }

}
