package landscape.entity

import com.eaio.uuid.UUID

/**
 * Created with IntelliJ IDEA.
 * User: mwielocha
 * Date: 12.11.2013
 * Time: 22:04
 * To change this template use File | Settings | File Templates.
 */
trait Entity[+E <: Entity[E]] {

  def uuidOpt: Option[UUID]

  def uuid: UUID = uuidOpt.get

  def withUuid(uuid: UUID): E

}
