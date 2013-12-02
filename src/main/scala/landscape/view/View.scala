package landscape.view

import landscape.entity.Entity
import com.netflix.astyanax.model.ColumnFamily
import landscape.serialization.EntitySerializer
import com.netflix.astyanax.{Keyspace, MutationBatch}
import scala.util.{Failure, Success}
import landscape.common.Logging

/**
 * author mikwie
 *
 */
class View[E <: Entity[E], K, C](val rowKeyMapper: E => Seq[K], columnNameMapper: E => Seq[C])
                                (implicit val keyspace: Keyspace, val viewCf: ColumnFamily[K, C]) extends Logging {

  import scalastyanax.Scalastyanax._

  def addOrUpdate(entity: E)(implicit serializer: EntitySerializer[E], batch: MutationBatch): MutationBatch = {
    keyspace.withMutationBatch(batch) { implicit batch =>
      for {
        rowKey <- rowKeyMapper(entity)
        columnName <- columnNameMapper(entity)
      } yield {
        viewCf ++= (rowKey -> columnName -> serializer.serialize(entity))
      }
    }
  }

  def remove(entity: E)(implicit batch: MutationBatch): MutationBatch = {
    keyspace.withMutationBatch(batch) { implicit batch =>
      for {
        rowKey <- rowKeyMapper(entity)
        columnName <- columnNameMapper(entity)
      } yield {
        viewCf --= (rowKey -> columnName)
      }
    }
  }
  
  def find(rowKey: K, columnName: C)(implicit serializer: EntitySerializer[E]): Option[E] = {
    viewCf(rowKey -> columnName).get match {
      case Success(result) => result.getResult.map[String, E](serializer.deserialize(_))
      case Failure(throwable) => {
        logger.warn(s"Error on entity fetch from view, cause: ${throwable.getMessage}")
        None
      }
    }
  }
}

case class ViewBuilder[E <: Entity[E], K, C](keyspace: Keyspace,
                                     columnFamilyOpt: Option[ColumnFamily[K, C]] = None,
                                     rowKeyMapperOpt: Option[E => Seq[K]] = None,
                                     columnNameMapperOpt: Option[E => Seq[C]] = None) {



  def withColumnFamily(columnFamily: ColumnFamily[K, C]) = copy(columnFamilyOpt = Some(columnFamily))

  def withRowKeyMapper(mapper: E => Seq[K]) = copy(rowKeyMapperOpt = Some(mapper))

  def withSingleRowKeyMapper(mapper: E => K) = {
    withRowKeyMapper(entity => Seq(mapper(entity)))
  }

  def withColumNameMapper(mapper: E => Seq[C]) = copy(columnNameMapperOpt = Some(mapper))

  def withSingleColumnNameMapper(mapper: E => C) = {
    withColumNameMapper(entity => Seq(mapper(entity)))
  }

  def build: View[E, K, C] = {
    (for {
      columFamily <- columnFamilyOpt
      rowKeyMapper <- rowKeyMapperOpt
      columnNameMapper <- columnNameMapperOpt
    } yield {
      new View[E, K, C](rowKeyMapper, columnNameMapper)(keyspace, columFamily)
    }).getOrElse(throw new IllegalStateException("Not enought arguments to build a view"))
  }
}
