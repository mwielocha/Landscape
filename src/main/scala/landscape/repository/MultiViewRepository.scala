package landscape.repository

import landscape.entity.Entity
import com.netflix.astyanax.model.ColumnFamily
import com.eaio.uuid.UUID
import landscape.common.UUIDSerializer._
import com.netflix.astyanax.serializers.StringSerializer
import com.netflix.astyanax.{MutationBatch, Keyspace}
import landscape.serialization.EntitySerializer
import landscape.view.View
import scala.util.{Failure, Success}
import landscape.common.{UUIDHelper, UUIDSerializer, Logging}
import com.google.common.collect.ImmutableMap
import scala.collection.JavaConversions._

/**
 * author mikwie
 *
 */
abstract class MultiViewRepository[E <: Entity[E]](defaultKeyspace: Keyspace, storageCfName: String, createStorageCf: Boolean = true) extends Logging {

  import scalastyanax.Scalastyanax._

  def views: Seq[View[E, _, _]]

  // this is to prevent compilation errors in idea
  implicit def keyspace: Keyspace = defaultKeyspace

  protected[MultiViewRepository] val storageColumnName = "entity"

  protected[MultiViewRepository] val storageCf = new ColumnFamily[UUID, String](
    storageCfName,
    UUIDSerializer.instance,
    StringSerializer.get(),
    StringSerializer.get()
  )

  if(createStorageCf) {
    storageCf.create(
      "default_validation_class" -> "UTF8Type",
      "key_validation_class" -> "TimeUUIDType",
      "comparator_type" -> "UTF8Type")
  }

  def truncate {
    views.foreach(_.truncate)
    keyspace.truncateColumnFamily(storageCf)
  }

  def update(entity: E)(implicit serializer: EntitySerializer[E]): E = {

    val mutationBatch = keyspace.newMutationBatch { implicit batch =>
      storageCf ++= (entity.uuid -> storageColumnName -> serializer.serialize(entity))
    }

    find(entity.uuid) match {
      case Some(previousEntity) => removeFromViews(previousEntity, mutationBatch)
      case None => //nothing to do here...
    }

    mutationBatch.execute()

    addToViews(entity, serializer, keyspace.prepareMutationBatch()).execute()

    entity
  }

  def addOrUpdate(entity: E)(implicit serializer: EntitySerializer[E]): E = {
    entity.uuidOpt match {
      case None => addOrUpdate(entity.withUuid(UUIDHelper.generate))
      case Some(uuid) => {
        update(entity)
      }
    }
  }

  def remove(entity: E) = {
    removeFromViews(entity, keyspace.newMutationBatch { implicit batch =>
      storageCf --= (entity.uuid -> storageColumnName)
    }).execute
  }

  protected[MultiViewRepository] def removeFromViews(entity: E, mutaionBatch: MutationBatch): MutationBatch = {
    views.foldLeft(mutaionBatch) {
      case (batch, view) => {
        view.remove(entity)(batch)
      }
    }
  }

  protected[MultiViewRepository] def addToViews(entity: E, serializer: EntitySerializer[E], mutaionBatch: MutationBatch): MutationBatch = {
    views.foldLeft(mutaionBatch) {
      case (batch, view) => view.addOrUpdate(entity)(serializer, mutaionBatch)
    }
  }

  def find(uuid: UUID)(implicit serializer: EntitySerializer[E]): Option[E] = {
    storageCf(uuid -> storageColumnName).get match {
      case Success(result) => result.getResult.map[String, E](value => serializer.deserialize(value))
      case Failure(throwable) => {
        logger.trace(s"Error on entity fetch from storage, cause: ${throwable.getMessage}")
        None
      }
    }
  }
}
