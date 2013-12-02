package landscape.model

import landscape.entity.{Entity}
import com.eaio.uuid.UUID
import landscape.common.UUIDSerializer
import org.scalatest.{ Matchers, WordSpec }
import UUIDSerializer._
import landscape.serialization.{Serializer, EntitySerializer}
import org.json.simple.JSONObject
import com.google.common.collect.ImmutableMap
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Created with IntelliJ IDEA.
 * User: mwielocha
 * Date: 12.11.2013
 * Time: 22:14
 * To change this template use File | Settings | File Templates.
 */
case class User(val uuidOpt: Option[UUID], name: String) extends Entity[User] {
  def withUuid(uuid: UUID): User = copy(uuidOpt = Some(uuid))
}

object User {

  implicit object UserSerializer extends EntitySerializer[User] {

    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    def serialize(entity: User): String = JSONObject.toJSONString(ImmutableMap.of(
      "uuidOption", entity.uuid.asString, "name", entity.name))

    def deserialize(text: String): User = {
      val map = mapper.readValue[Map[String, String]](text)
      User(fromString(map("uuidOption")), map("name"))
    }
  }

}

class EntitySpec extends WordSpec with Matchers {



  "Entity" should {

    "set its uuid" in {

      val uuid = UUIDSerializer.generate

      User(None, "John").withUuid(uuid).uuidOpt shouldEqual Some(uuid)

    }

    "serialize and deserialize itself" in {

      val john = User(None, "John").withUuid(generate)

      val text = Serializer.serialize(john)
      Serializer.deserialize[User](text) shouldEqual john
    }
  }
}
