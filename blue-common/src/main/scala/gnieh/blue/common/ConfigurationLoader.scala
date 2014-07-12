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

import com.typesafe.config.Config

import org.osgi.framework.Bundle

/** Service that allows bundle to load the configuration of a module given its identifier
 *
 *  @author Lucas Satabin
 */
trait ConfigurationLoader {

  /** Load the configuration associated to the given module */
  def load(bundle: Bundle): Config

}
