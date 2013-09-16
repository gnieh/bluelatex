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

import org.scalatest._

/** Extend this class to implement a test scenario for a feature of \BlueLaTeX.
 *  It provides the environment that allows you to interact with the server and
 *  check that it runs ok.
 *
 *  @author Lucas Satabin
 */
abstract class BlueScenario extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  object mailbox extends Mailbox

  override def beforeAll() {
    mailbox.start()
  }

  override def afterAll() {
    mailbox.stop()
  }

}
