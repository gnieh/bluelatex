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
package unit

import org.scalatest._
import akka.util.Timeout
import akka.actor.{Actor, Props, PoisonPill, ActorSystem}
import akka.testkit.{TestKit, TestActorRef, ImplicitSender, TestProbe}

import common._

import scala.util.Try

import java.util.UUID

class EchoActor extends Actor {
  def receive = { case x => sender ! x }
}

class SimpleResourceDispatcher extends ResourceDispatcher {
  var createdActors: Int = 0
  override def props(username: String, resourceid: String) = {
    createdActors += 1
    Try(Props[EchoActor])
  }
}

class DispatcherSpec extends TestKit(ActorSystem("DispatcherSpec"))
                     with ImplicitSender
                     with FeatureSpecLike
                     with BeforeAndAfterAll
                     with GivenWhenThen
                     with ShouldMatchers {

  override def afterAll() { system.shutdown() }
  implicit val timeout: Timeout = 1

  var currentPaperId: Int = 0
  var currentDispatcherId: Int = 0

  private def paperId(): String = {
    currentPaperId += 1
    currentPaperId.toString
  }

  private def dispatcherId(): String = {
    currentDispatcherId += 1
    s"Dispatcher${currentDispatcherId}"
  }

  feature("A dispatcher should dispatch messages in the \\BlueLaTeX engine") {

    scenario("a user opens a paper for edition") {

      Given("a dispatcher")
      val dispatcherName = dispatcherId
      val dispatcher = TestActorRef[SimpleResourceDispatcher](dispatcherName)

      And("a user")
      val user = "user"

      And("a paper")
      val paper = paperId

      When("he opens his paper")
      dispatcher ! Join(user, paper)

      Then("the dispatcher should have created an actor")
      dispatcher.underlyingActor.createdActors should be (1)
    }

    scenario("a message is sent to a paper") {

      Given("a dispatcher")
      val dispatcherName = dispatcherId
      val dispatcher = TestActorRef[SimpleResourceDispatcher](dispatcherName)

      And("one opened paper")
      val user = "user"
      val paper = paperId
      dispatcher ! Join(user, paper)

      When("a message is sent to the paper")
      dispatcher ! Forward(paper, "Message")

      Then("the dispatcher should forward the message to the paper")
      expectMsg("Message")

   }

    scenario("two users open the same paper for edition") {

      Given("a dispatcher")
      val dispatcherName = dispatcherId
      val dispatcher = TestActorRef[SimpleResourceDispatcher](dispatcherName)

      And("two users")
      val user1 = "user1"
      val user2 = "user2"

      And("a paper")
      val paper = paperId

      When("they both open the paper")
      dispatcher ! Join(user1, paper)
      dispatcher ! Join(user2, paper)

      Then("the dispatcher should create only one valid actor")
      dispatcher.underlyingActor.createdActors should be (1)

   }

    scenario("all users leave the paper") {

      Given("a dispatcher")
      val dispatcherName = dispatcherId
      val dispatcher = TestActorRef[SimpleResourceDispatcher](dispatcherName)

      And("one opened paper by two users")
      val user1 = "user1"
      val user2 = "user2"
      val paper = paperId
      dispatcher ! Join(user1, paper)
      dispatcher ! Join(user2, paper)

      When("they both leave the paper")
      dispatcher ! Part(user1, paper)
      dispatcher ! Part(user2, paper)

      Then("the dispatcher should kill the actor")
      // TODO: replace `actorFor` by `actorSelection`:
      // val deadActor = system.actorSelection(s"/user/${dispatcherName}/${paper}").resolveOne.value
      val deadActor = system.actorFor(s"/user/${dispatcherName}/${paper}")
      deadActor should be ('terminated)

    }
  }
}

