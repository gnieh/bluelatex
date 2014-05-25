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

/** The different roles for a paper:
 *   - an author is simply an user listed as such for the paper,
 *   - a reviewer is an user authenticated user listed as such for the paper,
 *   - other authenticated users have other role,
 *   - unauthenticated user get the anonymous role.
 *
 *  @author Lucas Satabin
 *
 */
sealed trait Role
case object Author extends Role
case object Reviewer extends Role
case object Other extends Role
case object Guest extends Role
case object Anonymous extends Role
