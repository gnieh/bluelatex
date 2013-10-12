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
package sync
package impl
package unit

import org.scalatest._

import akka.util.Timeout
import akka.actor.{Actor, Props, PoisonPill, ActorSystem}
import akka.testkit.{TestKit, TestActorRef, ImplicitSender, TestProbe}

import net.liftweb.json._
import net.liftweb.json.Serialization

import com.typesafe.config.ConfigFactory
import org.osgi.service.log.LogService

import name.fraser.neil.plaintext.DiffMatchPatch

import gnieh.blue.common.PaperConfiguration

import gnieh.blue.sync.impl.store.Store

class DummyLogger extends LogService {

  import  org.osgi.framework.ServiceReference

  def log(level: Int, message: String): Unit = {
    Console.println(message)
  }

  def log(level: Int, message: String, e: Throwable): Unit = {
    log(level, message)
  }

  def log(r: ServiceReference[_], level: Int, message: String): Unit = {
    log(level, message)
  }

  def log(r: ServiceReference[_], level: Int, message: String, e: Throwable): Unit= {
    log(level, message)
  }

}

class DummyStore extends Store {
  import scala.collection.mutable.Map

  var documents = Map.empty[String, String].withDefaultValue("")

  def save(document: Document): Unit = {
    documents += document.path -> document.text
  }

  def load(documentPath: String): Document = new Document(documentPath, documents(documentPath))

  def delete(document: Document) = documents.remove(document.path)
}


class SyncActorSpec extends TestKit(ActorSystem("SyncActorSpec"))
                            with ImplicitSender
                            with FeatureSpecLike
                            with BeforeAndAfterAll
                            with GivenWhenThen
                            with ShouldMatchers {

  private implicit val formats = DefaultFormats +
                                 new SyncSessionSerializer +
                                 new SyncCommandSerializer +
                                 new SyncActionSerializer +
                                 new EditSerializer

  override def afterAll() { system.shutdown() }
  implicit val timeout: Timeout = 1

  val config = new PaperConfiguration(ConfigFactory.load())
  val dmp = new DiffMatchPatch
  val store = new DummyStore
  val logger = new DummyLogger

  feature("A synchronization actor should handle commands from one client") {

    scenario("a user sends a delta command on an empty file") {

      Given("a fresh synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a delta from user")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Delta(0, List(Add("Hello")), false))))

      When("he sends the message")
      syncActor ! request

      Then("the actor should process the delta and sends back a response")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5)), false)))))

    }

    scenario("a user sends a delta command on a file he already used") {

      Given("a synchronization actor, already used by a user")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))
      syncActor ! SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Delta(0, List(Add("Hello")), false))))
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5)), false)))))

      And("a delta from the same user")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(1, List(Equality(5),Add(" World")), false))))

      When("he sends the message")
      syncActor ! request

      Then("the actor should process the delta and sends back a response")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 2, Delta(1, List(Equality(11)), false)))))

    }

    scenario("a new user send a delta command an existing file") {

      Given("a synchronization actor, already used by a user")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))
      syncActor ! SyncSession("user1", "paperId", List(SyncCommand("testPaper", 0, Delta(0, List(Add("Hello")), false))))
      expectMsg(SyncSession("user1", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5)), false)))))

      And("a delta from a new user")
      val request = SyncSession("user2", "paperId", List(SyncCommand("testPaper", 0, Delta(0, List(Add("World")), false))))

      When("he sends the message")
      syncActor ! request

      Then("the actor should process the delta and sends back a response")
      expectMsg(SyncSession("user2", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5), Add("Hello")), false)))))

    }

    scenario("a user sends a synchronization command with invalid server revision on empty file") {

      Given("a synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a synchronization command with invalid server revision")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 51, Delta(0, List(), false))))

      When("he sends the message")
      syncActor ! request

      Then("the actor should detect the invalid revision and send Raw version")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Raw(0, "", false)))))

    }

    scenario("a user sends a synchronization command with invalid server revision on existing file") {

      Given("a synchronization actor, already used by a user")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))
      syncActor ! SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Delta(0, List(Add("Hello")), false))))
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5)), false)))))

      And("a synchronization command with invalid server revision")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 51, Delta(0, List(), false))))

      When("he sends the message")
      syncActor ! request

      Then("the actor should detect the invalid revision and send Raw version")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Raw(1, "Hello", true)))))

    }

    scenario("persists an existing file on disk") {

      Given("a synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a user sending commands to the actor")
      syncActor ! SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Delta(0, List(Add("Hello")), false))))
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5)), false)))))

      When("a persist command is issues")
      syncActor ! PersistPaper

      Then("the actor should write the file to the disk")
      store.documents.size should be(1)
      store.documents(config.resource("paperId","testPaper").getCanonicalPath) should be("Hello")

    }
  }
}

