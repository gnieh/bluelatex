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
package impl

import org.fusesource.scalate._
import org.fusesource.scalate.support._
import org.fusesource.scalate.util._

import java.io.{
  File,
  FileNotFoundException
}

class TemplatesImpl(configuration: BlueConfiguration) extends Templates {

  import common.FileProcessing._

  val engine = {
    val engine = new TemplateEngine
    engine.templateDirectories = List(configuration.templateDir.getCanonicalPath)
    engine.resourceLoader = new FileResourceLoader {
      override def resource(uri: String) =
        super.resource(new File(configuration.templateDir, uri).getCanonicalPath)
    }
    engine
  }

  // used to set the correct classloader
  private val scope = org.fusesource.scalate.mustache.Scope

  def layout(name: String, params: (String, Any)*) = {
    val previousCl = Thread.currentThread.getContextClassLoader
    try {
      Thread.currentThread.setContextClassLoader(scope.getClass.getClassLoader)
      engine.layout(s"$name.mustache", Map(params: _*))
    } finally {
      Thread.currentThread.setContextClassLoader(previousCl)
    }
  }

}

