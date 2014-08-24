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
package impl

import java.net.{
  URLClassLoader,
  URL
}
import java.io.File

import java.util.Enumeration

import com.typesafe.config._

import org.osgi.framework.Bundle
import org.osgi.framework.wiring._

import scala.collection.JavaConverters._

import scala.annotation.tailrec

class ConfigurationLoaderImpl(commonName: String, val base: File) extends ConfigurationLoader {

  import FileUtils._

  def load(bundle: Bundle): Config = {
    val bundles = transitiveBundles(bundle)
    val classLoader =
      new ClassLoader(new URLClassLoader(bundles.map(b =>
          (base / b.getSymbolicName).toURI.toURL).toArray :+ (base / bundle.getSymbolicName).toURI.toURL)) {
      override def findResource(name: String): URL = {
        def tryGetResource(bundle: Bundle): Option[URL] =
          Option(bundle.getResource(name))

        val cls = bundles.foldLeft(None: Option[URL]) { (cls, bundle) => cls orElse tryGetResource(bundle) }
        cls getOrElse getParent.getResource(name)
      }

     override def findResources(name: String): Enumeration[URL] = {
       val resources = bundles.map { bundle => Option(bundle.getResources(name)).map { _.asScala.toList }.getOrElse(Nil) }.flatten
       java.util.Collections.enumeration(resources.asJava)
     }
    }
    ConfigFactory.load(classLoader)
  }

  private def transitiveBundles(b: Bundle): Set[Bundle] = {
    @tailrec
    def process(processed: Set[Bundle], remaining: Set[Bundle]): Set[Bundle] = {
      if (remaining.isEmpty) {
        processed
      } else {
        val (bundle, rest) = (remaining.head, remaining.tail)
        if (processed contains bundle) {
          process(processed, rest)
        } else {
          val wiring = bundle.adapt(classOf[BundleWiring])
          val requiredWires: List[BundleWire] = wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE).asScala.toList
          val direct: Set[Bundle] = requiredWires.flatMap { wire => Option(wire.getProviderWiring) map { _.getBundle } }.toSet
          process(processed + bundle, rest ++ (direct -- processed))
        }
      }
    }
    process(Set.empty, Set(b))
  }

}
