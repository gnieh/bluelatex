package gnieh.blue
package common

import akka.actor._

import scala.collection.mutable.{
  HashMap,
  MultiMap,
  Set
}

import scala.util.Try

final case class Join(username: String, resourceid: String)
final case class Part(username: String, resourceid: Option[String])
final case class Forward(resourceid: String, msg: Any)
case object Stop

/** A dispacther that instantiates an actor per resource name.
 *  The instance lives as long as at least one user is using this resource.
 *  Join/Part messages are sent automatically, a user only needs to take care
 *  to send `Forward` messages
 *
 *  @author Lucas Satabin
 */
abstract class ResourceDispatcher extends Actor {

  // count the number of users using a given resource by name
  private val users = new HashMap[String, Set[String]] with MultiMap[String, String]
  // the set of resources a user is connected to
  private val resources = new HashMap[String, Set[String]] with MultiMap[String, String]

  override def preStart(): Unit = {
    // subscribe to the dispatcher events
    context.system.eventStream.subscribe(self, classOf[Join])
    context.system.eventStream.subscribe(self, classOf[Part])
  }

  override def postStop(): Unit = {
    // unsubscribe from the dispatcher events
    context.system.eventStream.unsubscribe(self, classOf[Join])
    context.system.eventStream.unsubscribe(self, classOf[Part])
  }

  final def receive = {
    case join @ Join(username, resourceid) =>
      // create the actor if nobody uses this resource
      if(!users.contains(resourceid) || users(resourceid).size == 0) {
        for(props <- props(username, resourceid))
          context.watch(context.actorOf(props, name = resourceid))
        users(resourceid) = Set()
      }

      if(!users(resourceid).contains(username)) {
        // resent the Join message to the resource
        context.actorSelection(resourceid) ! join
      }

      users.addBinding(resourceid, username)
      resources.addBinding(username, resourceid)

    case msg @ Part(username, None) =>
      // for all resources the user is connected to, leave it
      for(resourceid <- resources.get(username).getOrElse(Set())) {
        part(username, resourceid, msg)
      }

    case msg @ Part(username, Some(resourceid)) =>
      // notify only the corresponding resource
      part(username, resourceid, msg)

    case Forward(resourceid, msg) if users.contains(resourceid) =>
      // forward the actual message to the resource instance
      context.actorSelection(resourceid).tell(msg, sender)

    case Forward(resourceid, msg) =>
      // execute the handler for unknown target resource
      unknownReceiver(resourceid, msg)

    case Stop =>
      context.become(stopping)
      // dispatch the Stop message to all managed actors
      context.actorSelection("*") ! Stop

    case Terminated(actor) =>
      // a managed actor was terminated, remove it from the set of managed resources
      terminated(actor.path.name)

  }

  val stopping: Receive = {

    case Terminated(actor) =>
      // a managed actor was terminated, remove it from the set of managed resources
      terminated(actor.path.name)
      // when all our managed actors terminated, kill ourselves
      if(users.isEmpty)
        self ! PoisonPill

    case Forward(resourceid, msg) =>
      // any forward message is sent to the unknown resource hook
      unknownReceiver(resourceid, msg)

  }

  /** Implement this method to specify how an actor for a yet unknown resource is created */
  def props(username: String, resourceid: String): Try[Props]

  private def part(username: String, resourceid: String, msg: Part): Unit = {
    val actor = context.actorSelection(resourceid)

    if(users.contains(resourceid) && users(resourceid).size == 1) {
      // this was the last user on this resource, kill it
      // after having performed proper cleanup if needed
      actor ! Stop
      actor ! PoisonPill
      users -= resourceid
    } else if(users.contains(resourceid) && users(resourceid).contains(username)) {
      // just tell the user left
      actor ! msg
      users(resourceid) -= username
    }
    if(resources.contains(username) && resources(username).size == 1) {
      // this was the last resource the user is connected to
      resources -= username
    } else if(resources.contains(username)) {
      resources(username) -= resourceid
    }
  }

  private def terminated(resourceid: String): Unit = {
    // remove this resource from the map of resource to connected users
    users -= resourceid
    // remove this resource from the resources each user if connected to
    for((username, _) <- resources)
      resources(username) -= resourceid
  }

  /** This method is called when the resource to forward the message is unknown.
   *  Override this to handle such undelivered message as you wish. By default dows nothing */
  protected def unknownReceiver(resourceid: String, msg: Any): Unit = {
  }

}
