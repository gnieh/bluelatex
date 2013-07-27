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
package sync
package impl

import net.liftweb.json._

/** Serialize/deserialize JSON message to SyncCommand.
 *  The general JSON format is as follows:
 *  ```
 *  { "name": "delta",
 *    "revision": 4,
 *    "data": ["=100", "-2", "+test", "=34"],
 *    "overwrite": false
 *  }
 *  ```
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 *
 */ 
class SyncCommandSerializer extends Serializer[SyncCommand] {
  private val _syncCommandClass = classOf[SyncCommand]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), SyncCommand] = {
    case (TypeInfo(_syncCommandClass, _), json) => json match {
      case JObject(fields) =>
        (
        for {
          JString("delta") <- (json \ "name").toOpt
          JInt(rev) <- (json \ "revision").toOpt
          JArray(edits) <- (json \ "data").toOpt
          JBool(overwrite) <- (json \ "overwrite").toOpt
        } yield Delta(rev.longValue, edits flatMap (_.extractOpt[Edit]), overwrite)).orElse(
        for {
          JString("raw") <- (json \ "name").toOpt
          JInt(rev) <- (json \ "clientRevision").toOpt
          JString(data) <- (json \ "data").toOpt
          JBool(overwrite) <- (json \ "overwrite").toOpt
        } yield Raw(rev.longValue, data, overwrite)).orElse(
        for {
          JString("message") <- (json \ "name").toOpt
          obj @ JObject(_) <- (json \ "json").toOpt
        } yield Message(obj, (json \ "from").extractOpt[String])).orElse(
        for {
          JString("nullify") <- (json \ "name").toOpt
        } yield Nullify).getOrElse(throw new MappingException(s"Can't convert $json to SyncCommand"))
      case _ => throw new MappingException(s"Can't convert $json to SyncCommand")
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: SyncCommand => Extraction.decompose(x)
  }
}

/** Serialize/deserialize JSON message to Edit.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 *
 */ 
class EditSerializer extends Serializer[Edit] {
  private val _editClass = classOf[Edit]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Edit] = {
    case (TypeInfo(_editClass, _), json) => json match {
      case JString(Edit(e)) => e
      case _ => throw new MappingException(s"Can't convert $json to Edit")
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: Edit => JString(x.toString)
  }
}
