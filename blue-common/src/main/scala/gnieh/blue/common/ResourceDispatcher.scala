package gnieh.blue
package common

import akka.actor._

import scala.collection.mutable.{
  Map,
  Set
}

final case class Join(username: String, resourceid: String)
final case class Part(username: String, resourceid: String)
final case class Forward(resourceid: String, msg: Any)

/** A dispacther that instantiates an actor per resource name.
 *  The instance lives as long as at least one user is using this resource.
 *  Join/Part messages are sent automatically, a user only needs to take care
 *  to send `Forward` messages
 *
 *  @author Lucas Satabin
 */
abstract class ResourceDispatcher extends Actor {

  // count the number of users using a given resource by name
  private val users = Map.empty[String, Set[String]].withDefaultValue(Set())

  final def receive = {
    case join @ Join(username, resourceid) =>
      // create the actor if nobody uses this resource
      if(users(resourceid).size == 0) {
        for(props <- props(username, resourceid))
          context.actorOf(props, name = resourceid)
        users(resourceid) = Set(username)
      } else if(!users(resourceid).contains(username)) {
        users(resourceid) += username
        // resent the Join message to the resource
        context.actorSelection(resourceid) ! join
      }

    case part @ Part(username, resourceid) =>
      val actor = context.actorSelection(resourceid)
      if(users(resourceid).size == 1) {
        // this was the last user on this resource, kill it
        actor ! PoisonPill
        users -= resourceid
      } else if(users(resourceid).contains(username)) {
        // just tell the user left
        actor ! part
        users(resourceid) -= username
      }
    case Forward(resourceid, msg) if users.contains(resourceid) =>
      // forward the actual message to the resource instance
      context.actorSelection(resourceid).tell(msg, sender)
  }

  def props(username: String, resourceid: String): Try[Props]

}
