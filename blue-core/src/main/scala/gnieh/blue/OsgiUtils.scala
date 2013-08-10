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

import org.osgi.framework.BundleContext

/** Exposes implicit conversions to rich version of the standard OSGi elements.
 *  This rich versions allows for a monadic programming style.
 *
 *  @author Lucas Satabin
 */
object OsgiUtils {

  implicit class RichContext(val context: BundleContext) extends AnyVal {

    /** Returns a rich version of a service reference */
    def get[T: Manifest]: RichService[T] =
      new RichService(context, implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[T]])

  }

}

class RichService[T](context: BundleContext, clazz: Class[T]) {
  self =>

  def map[U](fun: T => U): Option[U] = {
    val ref = context.getServiceReference(clazz)
    if(ref != null) {
      val service = context.getService(ref)
      if(service != null) try {
        Some(fun(service))
      } finally {
        context.ungetService(ref)
      } else {
        None
      }
    } else {
      None
    }
  }

  def flatMap[U](fun: T => Option[U]): Option[U] = {
    val ref = context.getServiceReference(clazz)
    if(ref != null) {
      val service = context.getService(ref)
      if(service != null) try {
        fun(service)
      } finally {
        context.ungetService(ref)
      } else {
        None
      }
    } else {
      None
    }
  }

  def filter(pred: T => Boolean): Option[T] =
    flatMap(s => if(pred(s)) Some(s) else None)

  def withFilter(pred: T => Boolean): WithFilter =
    new WithFilter(pred)

  class WithFilter(pred: T => Boolean) {
    def map[U](fun: T => U): Option[U] =
      self.flatMap(s => if(pred(s)) Some(fun(s)) else None)
    def flatMap[U](fun: T => Option[U]): Option[U] =
      self.flatMap(s => if(pred(s)) fun(s) else None)
    def foreach(fun: T => Unit): Unit =
      self.foreach(s => if(pred(s)) fun(s))
    def filter(p: T => Boolean): WithFilter =
      new WithFilter(s => pred(s) && p(s))
  }

  def foreach(fun: T => Unit): Unit = {
    val ref = context.getServiceReference(clazz)
    if(ref != null) {
      val service = context.getService(ref)
      if(service != null) try {
        fun(service)
      } finally {
        context.ungetService(ref)
      }
    }
  }

}
