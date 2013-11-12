package landscape.serialization


/**
 * Created with IntelliJ IDEA.
 * User: mwielocha
 * Date: 12.11.2013
 * Time: 23:17
 * To change this template use File | Settings | File Templates.
 */
object Serializer {

  def serialize[T : EntitySerializer](entity: T) = implicitly[EntitySerializer[T]].serialize(entity)

  def deserialize[T : EntitySerializer](text: String): T = implicitly[EntitySerializer[T]].deserialize(text)

}
