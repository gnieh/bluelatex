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

/** Internal system messages, used by the various bundles.
 *
 *  @author Audric Schiltknecht
 */
sealed trait SystemMessage

/** Sent when a (connected) user starts editing a file
 */
final case class Join(user: PaperRole, file: String) extends SystemMessage

/** Sent when a (connected) user ends editing a file
 */
final case class Part(user: PaperRole, file: String) extends SystemMessage
