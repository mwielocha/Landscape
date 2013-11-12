package landscape.serialization

/**
 * Created with IntelliJ IDEA.
 * User: mwielocha
 * Date: 12.11.2013
 * Time: 23:10
 * To change this template use File | Settings | File Templates.
 */
trait EntitySerializer[T] {

  def serialize(entity: T): String

  def deserialize(text: String): T

}
