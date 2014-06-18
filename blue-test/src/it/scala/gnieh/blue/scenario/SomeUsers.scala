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

import couch.User

import gnieh.sohva.CouchUser

/** Mix in this trait to add some users that are already registered in your database
 *  before executing each scenario
 *
 *  @author Lucas Satabin
 */
trait SomeUsers extends BeforeAndAfterEach {
  this: BlueScenario =>

  val gerard = Person("glambert", "Gérard", "Lambert", "gerard@lambert.org", Some("Gnieh Inc."))
  val prince = Person("pprince", "Petit", "Prince", "petit@prince.org", None)
  val rene = Person("rfallet", "René", "Fallet", "rene@falle.org", None)

  val predefinedPeople: List[Person]

  private def couchUsers =
    for(Person(username, _, _, _, _) <- predefinedPeople)
      yield CouchUser(username, username, List("blue_user"))

  private def  blueUsers =
    for(Person(username, firstName, lastName, emailAddress, affialiation) <- predefinedPeople)
      yield User(username, firstName, lastName, emailAddress, affialiation)

  override def beforeEach(): Unit = try {
    super.beforeEach()
  } finally {
    // save the couchdb users
    try {
      couch.database("_users").saveDocs(couchUsers)
    } catch {
      case e: Exception => e.printStackTrace
    }
    // save the \Blue users
    try {
      for(user <- blueUsers) {
        val userid = s"org.couchdb.user:${user.name}"
        userManager.create(userid, None)
        userManager.saveComponent(userid, user)
      }
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

  override def afterEach(): Unit = try {
    super.afterEach()
  } finally {
    // delete the couchdb users
    couch.database("_users").deleteDocs(couchUsers map (_._id))
    // delete the \Blue users
    for(user <- blueUsers)
      userManager.deleteEntity(s"org.couchdb.user:${user.name}")
  }

}

