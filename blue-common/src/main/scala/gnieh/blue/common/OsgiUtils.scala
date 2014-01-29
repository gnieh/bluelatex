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
package common

import org.osgi.framework.{
  BundleContext,
  ServiceReference,
  Constants
}
import org.osgi.util.tracker.ServiceTracker

import scala.collection.JavaConverters._

/** Exposes implicit conversions to rich version of the standard OSGi elements.
 *  This rich versions allows for a monadic programming style or more idiomatic code.
 *
 *  @author Lucas Satabin
 */
object OsgiUtils {

  implicit class RichContext(val context: BundleContext) extends AnyVal {

    /** Returns a rich version of a service reference */
    def get[T: Manifest]: Option[T] =
      for {
        ref <- Option(context.getServiceReference(implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[T]]))
        service <- Option(context.getService(ref))
      } yield service

    def getAll[T: Manifest]: Iterable[T] =
      for {
        ref <- context.getServiceReferences(implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[T]], null).asScala
        service <- Option(context.getService(ref))
      } yield service

    def trackOne[T: Manifest](handler: PartialFunction[TrackerEvent[T], Unit]): RichTracker[T] =
      new RichTracker(context, implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[T]], true, handler)

    def trackAll[T: Manifest](handler: PartialFunction[TrackerEvent[T], Unit]): RichTracker[T] =
      new RichTracker(context, implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[T]], false, handler)

  }

}

/** A tracker instance that allows people to program in a more idiomatic way for scala.
 *  It has two different mode:
 *   - '''singleton''' allowing people to track only one instance at a time.
 *     Once we received a `ServiceAdded` event, only new events concerning this particular
 *     service instance will be tracked. Upon `ServiceRemoved`, a new instance can be tracked,
 *   - ''all''' allowing people to track all instances of a service.
 *
 *  @author Lucas Satabin
 */
class RichTracker[T](context: BundleContext, clazz: Class[T], singleton: Boolean, handler: PartialFunction[TrackerEvent[T], Unit]) {

  // used to store the identifier of the service when in singleton mode
  private var tracked: Option[Long] = None

  private val tracker = new ServiceTracker[T, Long](context, clazz, null) {

    override def addingService(ref: ServiceReference[T]): Long = {
      // return the servive identifier
      val id = ref.getProperty(Constants.SERVICE_ID).asInstanceOf[Long]
      (singleton, tracked) match {
        case (false, _) | (true, None) =>
          // set the tracked id if singleton required
          if(singleton)
            tracked = Some(id)
          // no service tracked yet, track this one
          // get the service instance
          val service = context.getService(ref)
          // create the event
          val event = ServiceAdded(service)
          // notify the handler if defined for this event
          if(handler.isDefinedAt(event))
            handler(event)
        case (true, Some(_)) =>
          // already track one service and singleton required, ignore this one
      }
      id
    }

    override def removedService(ref: ServiceReference[T], id: Long): Unit =
      (singleton, tracked) match {
        case (false, _) | (true, Some(`id`)) =>
          // get the service instance
          val service = context.getService(ref)
          // create the event
          val event = ServiceRemoved(service)
          // notify the handler if defined for this event
          if(handler.isDefinedAt(event))
            handler(event)
          // if in singleton mode, make room for a new instance
          if(singleton)
            tracked = None
        case (true, _) =>
          // another service id, ignore it
      }

    override def modifiedService(ref: ServiceReference[T], id: Long): Unit =
      (singleton, tracked) match {
        case (false, _) | (true, Some(`id`)) =>
          // get the service instance
          val service = context.getService(ref)
          // create the event
          val event = ServiceUpdated(service)
          // notify the handler if defined for this event
          if(handler.isDefinedAt(event))
            handler(event)
        case (true, _) =>
          // another service id, ignore it
      }

  }

  tracker.open

  def close(): Unit =
    tracker.close()

}

sealed trait TrackerEvent[+T] {
  val service: T
}
final case class ServiceAdded[T](service: T) extends TrackerEvent[T]
final case class ServiceRemoved[T](service: T) extends TrackerEvent[T]
final case class ServiceUpdated[T](service: T) extends TrackerEvent[T]

