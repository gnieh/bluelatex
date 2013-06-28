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

import java.text.{ SimpleDateFormat, ParseException }

/** @author Lucas Satabin
 *
 */
package object util {

  // ======== Some useful extractors ========

  object dot {
    def unapply(input: String) = {
      val index = input.lastIndexOf('.')
      if (index > 0) {
        // there is at least tow elements
        Some((input.substring(0, index), input.substring(index + 1)))
      } else {
        None
      }
    }
  }

  object long {
    def unapply(input: String) = try {
      Some(input.toLong)
    } catch {
      case _: Exception => None
    }
  }

  object int {
    def unapply(input: String) = try {
      Some(input.toInt)
    } catch {
      case _: Exception => None
    }
  }

  object date {
    val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    def unapply(input: String) = try {
      Option(formatter.parse(input))
    } catch {
      case _: ParseException =>
        None
    }
  }

}
