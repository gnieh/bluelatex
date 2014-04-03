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

import scala.concurrent.{Promise, Await}
import scala.concurrent.duration.Duration

import net.liftweb.json._
import net.liftweb.json.Serialization

import com.typesafe.config.ConfigFactory
import org.osgi.service.log.LogService

import name.fraser.neil.plaintext.DiffMatchPatch

import java.util.{Date, Calendar}

import gnieh.blue.common.PaperConfiguration
import gnieh.blue.common.{Join, Part}

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

  def clear(): Unit = documents.clear()

  def get(documentPath: String): Option[String] = documents.get(documentPath)
}


class SyncActorSpec extends TestKit(ActorSystem("SyncActorSpec"))
                            with ImplicitSender
                            with FeatureSpecLike
                            with BeforeAndAfterAll
                            with BeforeAndAfterEach
                            with GivenWhenThen
                            with ShouldMatchers {

  private implicit val formats = DefaultFormats +
                                 new SyncSessionSerializer +
                                 new SyncCommandSerializer +
                                 new MessageSerializer +
                                 new SyncActionSerializer +
                                 new EditSerializer

  override def afterAll() { system.shutdown() }
  override def beforeEach() { store.clear() }
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

    scenario("a user sends a raw command on an empty file") {

      Given("a fresh synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a delta from user")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Raw(0, "", false))))

      When("he sends the message")
      syncActor ! request

      Then("the actor should process the raw and sends back a response")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(), false)))))

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
      val p = Promise[Unit]()
      syncActor ! PersistPaper(p)

      Then("the actor should write the file to the disk")
      Await.result(p.future, Duration.Inf)
      store.documents.size should be(1)
      store.documents(config.resource("paperId","testPaper").getCanonicalPath) should be("Hello")

    }

    scenario("synchronization message for raw content with accent") {

      Given("a synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a raw request with accent from user")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Raw(0, "t%C3%AAte", false))))

      When("he sends the message and persist the file")
      val p = Promise[Unit]()
      syncActor ! request
      syncActor ! PersistPaper(p)

      Then("the actor should process the raw message and sends back a response")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(4)), false)))))

      And("the stored document should have the accent")
      Await.result(p.future, Duration.Inf)
      val documentText = store.get(config.resource("paperId","testPaper").getCanonicalPath)
      documentText should not be(None)
      documentText.get should be("tête")
    }

    scenario("synchronization message for delta content with accent") {

      Given("a synchronization actor with an existing paper")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))
      syncActor ! SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Raw(0, "Hello", false))))
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5)), false)))))

      And("a delta request with accent from user")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Delta(0, List(Delete(5), Add("t%C3%AAte")), false))))

      When("he sends the message and persist the file")
      val p = Promise[Unit]()
      syncActor ! request
      syncActor ! PersistPaper(p)

      Then("the actor should process the delta message and sends back a response")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(4)), false)))))

      And("the stored document should have the accent")
      Await.result(p.future, Duration.Inf)
      val documentText = store.get(config.resource("paperId","testPaper").getCanonicalPath)
      documentText should not be(None)
      documentText.get should be("tête")
    }

    scenario("error during synchronization session for content with accent") {

      Given("a synchronization actor with an existing paper")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))
      syncActor ! SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Raw(0, "t%C3%AAte", false))))
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(4)), false)))))

      And("a message with invalid revision for a the paper")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 42, Delta(0, List(Delete(5), Add("t%C3%AAte")), false))))

      When("the message is sent")
      syncActor ! request

      Then("the actor should detect the invalid revision and send Raw version")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Raw(1, "t%C3%AAte", true)))))
    }

    scenario("error during synchronization session for content with space") {

      Given("a synchronization actor with an existing paper")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))
      syncActor ! SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Raw(0, "My name is", false))))
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(10)), false)))))

      And("a message with invalid revision for a the paper")
      val request = SyncSession("user", "paperId", List(SyncCommand("testPaper", 42, Delta(0, List(Delete(5), Add("t%C3%AAte")), false))))

      When("the message is sent")
      syncActor ! request

      Then("the actor should detect the invalid revision and send Raw version")
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Raw(1, "My name is", true)))))
    }

    scenario("retrieving the last modification date for 'empty' paper should be possible") {

      Given("a synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      When("a user retrieves the last modification date")
      val p = Promise[Date]()
      syncActor ! LastModificationDate(p)

      Then("the actor should always return a valid date (with a small epsilon to now())")
      val now = Calendar.getInstance().getTime()
      val modificationDate: Date = Await.result(p.future, Duration.Inf)
      val epsilon: Long = 500 /* 500 msec */
      (now.getTime() - modificationDate.getTime()) should be <= epsilon
    }

    scenario("retrieving the last modification date for modified paper should return new date") {

      Given("a synchronization actor with an existing paper")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))
      syncActor ! SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Raw(0, "Hello", false))))
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5)), false)))))

      And("the last modification date")
      val p = Promise[Date]()
      syncActor ! LastModificationDate(p)
      val modificationDate: Date = Await.result(p.future, Duration.Inf)

      When("a user modifies the paper")
      syncActor ! SyncSession("user", "paperId", List(SyncCommand("testPaper", 0, Delta(0, List(Delete(5),Add("World")), false))))
      expectMsg(SyncSession("user", "paperId", List(SyncCommand("testPaper", 1, Delta(0, List(Equality(5)), false)))))

      Then("the actor should always return a new valid date (with after first date)")
      val pp = Promise[Date]()
      syncActor ! LastModificationDate(pp)
      val newModificationDate: Date = Await.result(pp.future, Duration.Inf)
      newModificationDate.after(modificationDate) should be (true)
    }
  }

  feature("A synchronization actor should handle broadcast messages between clients") {

    scenario("simple broadcast message") {

      Given("a synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a list of connected users")
      val connectedUsers = List("user1", "user2")
      connectedUsers foreach ( syncActor ! Join(_, "paperId") )

      When("a user sends a message")
      val user1 = """|{
                     |  "peerId": "user1",
                     |  "paperId": "paperId",
                     |  "commands": [
                     |    {
                     |      "from": "11th",
                     |      "json": {
                     |        "content": "Fezzes are cool"
                     |      },
                     |      "retrieve": false
                     |    }
                     |  ]
                     |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](user1)
      expectMsg(SyncSession("user1", "paperId", List()))

      And("an other user retrieves its messages")
      val user2 = """|{
                     |  "peerId": "user2",
                     |  "paperId": "paperId",
                     |  "commands": [
                     |    {
                     |      "from": "noone",
                     |      "json": {}
                     |      "retrieve": true
                     |    }
                     |  ]
                     |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](user2)

      Then("the actor should send all queued messages to the second user")
      expectMsg(SyncSession("user2", "paperId", List(Message("11th",
                                                             JObject(List(JField("content",
                                                                                 JString("Fezzes are cool")))),
                                                              false,
                                                              None))))

    }

    scenario("multiple broadcast messages") {

      Given("a synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a list of connected users")
      val connectedUsers = List("user1", "user2")
      connectedUsers foreach ( syncActor ! Join(_, "paperId") )

      When("a user sends multiple messages")
      val user1 = """|{
                     |  "peerId": "user1",
                     |  "paperId": "paperId",
                     |  "commands": [
                     |    {
                     |      "from": "11th",
                     |      "json": {
                     |        "content": "Fish fingers and custard"
                     |      },
                     |      "retrieve": false
                     |    },
                     |    {
                     |      "from": "10th",
                     |      "json": {
                     |        "content": "Always bring a banana to a party"
                     |      },
                     |      "retrieve": false
                     |    }
                     |  ]
                     |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](user1)
      expectMsg(SyncSession("user1", "paperId", List()))

      And("an other user retrieves its messages")
      val user2 = """|{
                     |  "peerId": "user2",
                     |  "paperId": "paperId",
                     |  "commands": [
                     |    {
                     |      "from": "noone",
                     |      "json": {},
                     |      "retrieve": true
                     |    }
                     |  ]
                     |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](user2)

      Then("the actor should send all queued messages to the second user")
      expectMsg(SyncSession("user2", "paperId", List(Message("11th",
                                                             JObject(List(JField("content",
                                                                                JString("Fish fingers and custard")))),
                                                              false,
                                                              None),
                                                      Message("10th",
                                                              JObject(List(JField("content",
                                                                                  JString("Always bring a banana to a party")))),
                                                              false,
                                                              None))))

    }

    scenario("broadcast messages and synchronization commands interlaced") {

      Given("a synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a list of connected users")
      val connectedUsers = List("user1", "user2")
      connectedUsers foreach ( syncActor ! Join(_, "paperId") )

      When("a user sends a sychronization command and a message")
      val user1 = """|{
                     |  "peerId": "user1",
                     |  "paperId": "paperId",
                     |  "commands": [
                     |    {
                     |      "filename": "file1.tex",
                     |      "revision": 0,
                     |      "action": {
                     |        "name": "raw",
                     |        "revision": 1,
                     |        "data": "Fish fingers and custard",
                     |        "overwrite": false,
                     |      }
                     |    },
                     |    {
                     |      "from": "11th",
                     |      "json": {
                     |        "content": "Hold tight and pretend it's a plan"
                     |      },
                     |      "retrieve": false
                     |    }
                     |  ]
                     |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](user1)
      expectMsg(SyncSession("user1", "paperId", List(SyncCommand("file1.tex", 1,
                                                                 Delta(0, List(Equality(24)),false)))))

      And("an other user retrieves its messages")
      val user2 = """|{
                     |  "peerId": "user2",
                     |  "paperId": "paperId",
                     |  "commands": [
                     |    {
                     |      "from": "noone",
                     |      "json": {},
                     |      "retrieve": true
                     |    }
                     |  ]
                     |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](user2)

      Then("the actor should send all queued messages to the second user")
      expectMsg(SyncSession("user2", "paperId", List(Message("11th",
                                                              JObject(List(JField("content",
                                                                                  JString("Hold tight and pretend it's a plan")))),
                                                              false,
                                                              None))))

      And("he should also be able to get the modified text")
      val synchro2 = """|{
                        |  "peerId": "user2",
                        |  "paperId": "paperId",
                        |  "commands": [
                        |    {
                        |      "filename": "file1.tex",
                        |      "revision": 0,
                        |      "action": {
                        |        "name": "raw",
                        |        "revision": 1,
                        |        "data": "",
                        |        "overwrite": false,
                        |      }
                        |    }
                        |  ]
                        |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](synchro2)

      expectMsg(SyncSession("user2", "paperId", List(SyncCommand("file1.tex", 1,
                                                                 Delta(0, List(Add("Fish fingers and custard")),false)))))

    }

    scenario("clean-up when client leaves") {

      Given("a synchronization actor")
      val syncActor = TestActorRef(new SyncActor(config, "paperId", store, dmp, logger))

      And("a list of connected users")
      val connectedUsers = List("user1", "user2")
      connectedUsers foreach ( syncActor ! Join(_, "paperId") )

      When("a user sends a message")
      val user1 = """|{
                     |  "peerId": "user1",
                     |  "paperId": "paperId",
                     |  "commands": [
                     |    {
                     |      "from": "11th",
                     |      "json": {
                     |        "content": "Fezzes are cool"
                     |      },
                     |      "retrieve": false
                     |    }
                     |  ]
                     |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](user1)
      expectMsg(SyncSession("user1", "paperId", List()))

      And("an other user leaves the server")
      syncActor ! Part("user2", Some("paperId"))

      When("he rejoins the server later")
      syncActor ! Join("user2", "paperId")

      And("he retrieves its messages")
      val user2 = """|{
                     |  "peerId": "user2",
                     |  "paperId": "paperId",
                     |  "commands": [
                     |    {
                     |      "from": "noone",
                     |      "json": {}
                     |      "retrieve": true
                     |    }
                     |  ]
                     |}""".stripMargin
      syncActor ! Serialization.read[SyncSession](user2)

      Then("he should have no message")
      expectMsg(SyncSession("user2", "paperId", List()))
    }

  }

}

