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
sealed abstract class Permission(val name: String) {
  def unapply(permissions: List[Permission]): Boolean =
    permissions.contains(this)
}

case object Publish extends Permission("publish")
case object Configure extends Permission("configure")
case object Edit extends Permission("edit")
case object Delete extends Permission("delete")
case object Compile extends Permission("compile")
case object Download extends Permission("download")
case object Read extends Permission("read")
case object View extends Permission("view")
case object Comment extends Permission("comment")
case object Chat extends Permission("chat")
case object Fork extends Permission("fork")
case object ChangePhase extends Permission("change-phase")
final case class Custom(override val name: String) extends Permission(name)

object Permission {

  def apply(name: String): Permission = name match {
    case "publish"      => Publish
    case "configure"    => Configure
    case "edit"         => Edit
    case "delete"       => Delete
    case "compile"      => Compile
    case "download"     => Download
    case "read"         => Read
    case "comment"      => Comment
    case "chat"         => Chat
    case "fork"         => Fork
    case "change-phase" => ChangePhase
    case _              => Custom(name)
  }

  def unapply(x: Any) = x match {
    case p: Permission => Some(p.name)
    case _             => None
  }

}
