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
package couch

import gnieh.sohva.IdRev

import permission._
import common.{
  UserInfo,
  SingletonSet,
  EmptySet
}

/** Role component that can be attached to a paper entity.
 *  It lists the roles for users and/or groups.
 *
 *  @author Lucas Satabin
 */
case class PaperRole(_id: String, authors: UsersGroups, reviewers: UsersGroups, guests: UsersGroups) extends IdRev {

  def roleOf(user: Option[UserInfo]): Role = user match {
    case Some(userCtx) if authors.contains(userCtx)   => Author
    case Some(userCtx) if reviewers.contains(userCtx) => Reviewer
    case Some(userCtx) if guests.contains(userCtx)    => Guest
    case Some(userCtx)                                => Other
    case None                                         => Anonymous
  }

}

object SingleAuthor {
  def unapply(roles: PaperRole): Option[String] =
    roles.authors match {
      case UsersGroups(SingletonSet(author), EmptySet()) => Some(author)
      case _                              => None
    }
}

case class UsersGroups(users: Set[String], groups: Set[String]) {

  /** A user belongs to a role if he is either explicitly listed in the user list
   *  or if he belongs to a group that is listed in the group list */
  def contains(user: UserInfo): Boolean =
    users.contains(user.name) || groups.intersect(user.roles.toSet).nonEmpty

  def -(user: UserInfo): UsersGroups =
    this.copy(users - user.name)

}
