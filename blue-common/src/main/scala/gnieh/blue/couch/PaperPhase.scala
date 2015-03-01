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

import permission.{
  Phase,
  Role,
  Permission
}

/** Phase component that can be attached to a paper entity.
 *  It associates the current phase to a paper and the next allowed phases as well
 *  as the permissions for this phase.
 *
 *  @author Lucas Satabin
 */
case class PaperPhase(_id: String, phase: String, permissions: Map[String, List[Permission]], next: List[Phase]) extends IdRev
