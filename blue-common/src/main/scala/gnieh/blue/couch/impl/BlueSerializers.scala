package gnieh.blue
package couch
package impl

import net.liftweb.json._

import gnieh.sohva.SohvaSerializer

import permission._

object PermissionSerializer extends Serializer[Permission] {

  val PermissionClass = classOf[Permission]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Permission] = {
    case (TypeInfo(PermissionClass, _), JString(name)) =>
      Permission(name)
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case Permission(name) => JString(name)
  }

}

object BluePermissionSerializer extends SohvaSerializer[Permission] {
  def serializer(v: String) =
    PermissionSerializer
}
