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

class SyncSessionSerializer extends Serializer[SyncSession] {
  private val _syncSessionClass = classOf[SyncSession]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), SyncSession] = {
    case (TypeInfo(clazz, _), json @ JObject(_)) if clazz == _syncSessionClass =>
      (for {
        JString(peerId) <- (json \ "peerId").toOpt
        JString(paperId) <- (json \ "paperId").toOpt
        JArray(commands) <- (json \ "commands").toOpt
      } yield SyncSession(peerId, paperId, commands map { x => (x.extractOpt[Message]).getOrElse(x.extract[SyncCommand]) })).getOrElse(throw new MappingException(s"Can't convert $json to SyncSession"))
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: SyncSession => JObject(List(JField("peerId", JString(x.peerId)),
                                        JField("paperId", JString(x.paperId)),
                                        JField("commands", Extraction.decompose(x.commands))))
  }
}

/** Serialize/deserialize JSON message to Message.
 *
 *  @author Audric Schiltknecht
 *
 */
class MessageSerializer extends Serializer[Message] {
  private val _commandClass = classOf[Message]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Message] = {
    case (TypeInfo(clazz, _), json @ JObject(_)) if clazz == _commandClass =>
      (for {
        JString(from) <- (json \ "from").toOpt
        obj @ JObject(_) <- (json \ "json").toOpt
      } yield Message(from, obj, (json \ "filename").extractOpt[String])).getOrElse(throw new MappingException(s"Can't convert $json to Message"))
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: Message => {
      val tail = x.filename.map(
        f => List(JField("filename", JString(f)))
      ).getOrElse(Nil)
      JObject(JField("from", JString(x.from)) ::
              JField("json", x.json) ::
              tail)
    }
  }
}

/** Serialize/deserialize JSON message to Command.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 *
 */
class SyncCommandSerializer extends Serializer[SyncCommand] {
  private val _commandClass = classOf[SyncCommand]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), SyncCommand] = {
    case (TypeInfo(clazz, _), json @ JObject(_)) if clazz == _commandClass =>
      (for {
        JString(filename) <- (json \ "filename").toOpt
        JInt(rev) <- (json \ "revision").toOpt
        action @ JObject(_) <- (json \ "action").toOpt
      } yield SyncCommand(filename, rev.longValue, action.extract[SyncAction])).getOrElse(throw new MappingException(s"Can't convert $json to SyncCommand"))
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: SyncCommand => JObject(List(JField("filename", JString(x.filename)),
                                        JField("revision", JInt(x.revision)),
                                        JField("action", Extraction.decompose(x.action))))
  }
}

/** Serialize/deserialize JSON message to SyncAction.
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
class SyncActionSerializer extends Serializer[SyncAction] {
  private val _syncCommandClass = classOf[SyncAction]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), SyncAction] = {
    case (TypeInfo(clazz, _), json @ JObject(_)) if clazz == _syncCommandClass => {
      (for {
        JString("delta") <- (json \ "name").toOpt
        JInt(rev) <- (json \ "revision").toOpt
        JArray(edits) <- (json \ "data").toOpt
        JBool(overwrite) <- (json \ "overwrite").toOpt
      } yield Delta(rev.longValue, edits flatMap (_.extractOpt[Edit]), overwrite)).orElse(
      for {
        JString("raw") <- (json \ "name").toOpt
        JInt(rev) <- (json \ "revision").toOpt
        JString(data) <- (json \ "data").toOpt
        JBool(overwrite) <- (json \ "overwrite").toOpt
      } yield Raw(rev.longValue, data, overwrite)).orElse(
      for {
        JString("nullify") <- (json \ "name").toOpt
      } yield Nullify).getOrElse(throw new MappingException(s"Can't convert $json to SyncAction"))}
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case Nullify => JObject(List(JField("name", JString("nullify"))))
    case x: Raw => JObject(List(JField("name", JString("raw")),
                                JField("revision", JInt(x.revision)),
                                JField("data", JString(x.data)),
                                JField("overwrite", JBool(x.overwrite))))
    case x: Delta => JObject(List(JField("name", JString("delta")),
                                  JField("revision", JInt(x.revision)),
                                  JField("data", Extraction.decompose(x.data)),
                                  JField("overwrite", JBool(x.overwrite))))
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
    case (TypeInfo(clazz, _), json) if clazz == _editClass => {
      json match {
        case JString(Edit(e)) => e
        case _ => throw new MappingException(s"Can't convert $json to Edit")
      }}
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: Edit => JString(x.toString)
  }
}
