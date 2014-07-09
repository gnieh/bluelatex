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
package permission

/** A permission on paper that defines how certain user may interact on the paper
 *
 *  @author Lucas Satabin
 */
sealed trait Permission
case object Publish extends Permission
case object Configure extends Permission
case object Edit extends Permission
case object Compile extends Permission
case object Download extends Permission
case object Read extends Permission
case object Comment extends Permission
case object Chat extends Permission
case object Fork extends Permission
case object ChangePhase extends Permission

object Permission {

  def apply(name: String): Option[Permission] = name match {
    case "publish"      => Some(Publish)
    case "configure"    => Some(Configure)
    case "edit"         => Some(Edit)
    case "compile"      => Some(Compile)
    case "download"     => Some(Download)
    case "read"         => Some(Read)
    case "comment"      => Some(Comment)
    case "chat"         => Some(Chat)
    case "fork"         => Some(Fork)
    case "change-phase" => Some(ChangePhase)
    case _              => None
  }

}
