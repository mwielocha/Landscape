package landscape.entity

import com.eaio.uuid.UUID

/**
 * Created with IntelliJ IDEA.
 * User: mwielocha
 * Date: 12.11.2013
 * Time: 22:04
 * To change this template use File | Settings | File Templates.
 */
trait Entity[T] {

  def uuidOption: Option[UUID]

  def uuid: UUID = uuidOption.get

  def withUuid(uuid: UUID): T

}
