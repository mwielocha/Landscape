package landscape.common

import com.eaio.uuid.UUID
import org.joda.time.{DateTimeZone, DateTime}
import com.netflix.astyanax.serializers.{AbstractSerializer, TimeUUIDSerializer}
import java.nio.ByteBuffer

/**
 * Created with IntelliJ IDEA.
 * User: mwielocha
 * Date: 12.11.2013
 * Time: 22:00
 * To change this template use File | Settings | File Templates.
 */
object UUIDConversions {
  import scala.util.control.Exception._
  import com.eaio.uuid.UUIDGen

  implicit def eaio2String(uuid: UUID) = uuid.toString

  implicit def java2eaio(uuid: java.util.UUID) = new UUID(uuid.toString)
  implicit def eaio2java(uuid: UUID) = java.util.UUID.fromString(uuid.toString)

//  implicit val uuidReads = new Reads[UUID] {
//    override def reads(json: JsValue) = try {
//      JsSuccess(java2eaio(java.util.UUID.fromString(json.as[String])))
//    } catch {
//      case e: Exception => JsError(e.getClass.getSimpleName + ":" + e.getMessage)
//    }
//  }
//
//  implicit val uuidWrites = new Writes[UUID] {
//    override def writes(uuid: UUID) = {
//      Json.toJson(uuid.toString)
//    }
//  }

  implicit val ordering: Ordering[UUID] = Ordering.by (uuid => (toTime(uuid), uuid.getTime, uuid.getClockSeqAndNode))

  def generate = new UUID

  def fromString(uuid: String): Option[UUID] = allCatch.opt(new UUID(uuid))

  def fromDateTime(dt: DateTime): UUID = fromTime(dt.getMillis)

  def fromTime(timestamp: Long): UUID = {
    val timeEpoch = (timestamp * 10000) + UUID_EPOCH
    var time = 0L

    // time low
    time = timeEpoch << 32;

    // time mid
    time |= (timeEpoch & 0xFFFF00000000L) >> 16;

    // time hi and version
    time |= 0x1000 | ((timeEpoch >> 48) & 0x0FFF); // version 1

    new UUID(time, UUIDGen.getClockSeqAndNode)
  }

  def toTime(uuid: UUID): Long =
    ((java.util.UUID.fromString(uuid.toString).timestamp) - UUID_EPOCH) / 10000

  def toDateTime(uuid: UUID) = new DateTime(toTime(uuid), DateTimeZone.UTC)

  private final val UUID_EPOCH = 0x01B21DD213814000L;

  val eaioTimeUUIDSerializer = new AbstractSerializer[UUID] {

    val delegate = TimeUUIDSerializer.get()

    def toByteBuffer(in: UUID): ByteBuffer = delegate.toByteBuffer(in)

    def fromByteBuffer(in: ByteBuffer): UUID = delegate.fromByteBuffer(in)

    override def getNext(byteBuffer: ByteBuffer): ByteBuffer = delegate.getNext(byteBuffer)

    override def toBytes(obj: UUID): Array[Byte] = delegate.toBytes(obj)

    override def fromString(string: String): ByteBuffer = delegate.fromString(string)

    override def getString(byteBuffer: ByteBuffer): String = delegate.getString(byteBuffer)
  }

  implicit class RichUUID(val uuid: UUID) {

    def asMilis = toTime(uuid)

    def asDateTime = new DateTime(toTime(uuid))

    def asString = eaio2String(uuid)

    def asJava = java2eaio(uuid)

  }
}
