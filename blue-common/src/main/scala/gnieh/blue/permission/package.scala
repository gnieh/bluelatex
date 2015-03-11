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

package object permission {

  // some widely used 'built-in' permissions

  val Publish = Permission("publish")
  val Configure = Permission("configure")
  val Edit = Permission("edit")
  val Delete = Permission("delete")
  val Compile = Permission("compile")
  val Download = Permission("download")
  val Read = Permission("read")
  val View = Permission("view")
  val Comment = Permission("comment")
  val Chat = Permission("chat")
  val Fork = Permission("fork")
  val ChangePhase = Permission("change-phase")

}
