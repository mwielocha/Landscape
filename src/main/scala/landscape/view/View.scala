package landscape.view

import landscape.entity.Entity
import com.netflix.astyanax.model.{Row, ColumnFamily}
import landscape.serialization.EntitySerializer
import com.netflix.astyanax.{Keyspace, MutationBatch}
import scala.util.{Failure, Success}
import landscape.common.Logging
import com.eaio.uuid.UUID

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
        logger.debug(s"Error on entity fetch from view, cause: ${throwable.getMessage}")
        None
      }
    }
  }

  def findByRange(rowKey: K, column: Option[C], limit: Int)(implicit serializer: EntitySerializer[E], manifestK: Manifest[K], manifestC: Manifest[C]): Iterable[E] = {
    viewCf(rowKey -> from[C](column).take(limit)).get match {
      case Success(result) => result.getResult.flatMapValues[String, E](serializer.deserialize(_))
      case Failure(throwable) => {
        logger.debug(s"Error on entities fetch from view, cause: ${throwable.getMessage}")
        Nil
      }
    }
  }

  def findRow(rowKey: K)(implicit serializer: EntitySerializer[E], manifestK: Manifest[K], manifestC: Manifest[C]): Iterable[E] = {
    viewCf(rowKey).get match {
      case Success(row) => row.getResult.flatMapValues[String, E](serializer.deserialize(_))
      case Failure(throwable) => {
        logger.debug(s"Error on entities fetch from view, cause: ${throwable.getMessage}")
        Nil
      }
    }
  }

  def foreach(function: Row[K, C] => Unit): Unit = {
    viewCf.foreach(row => {
      function(row); true
    })
  }

  def truncate {
    keyspace.truncateColumnFamily(viewCf)
  }
}

case class ViewBuilder[E <: Entity[E], K, C](keyspace: Keyspace,
                                     columnFamilyOpt: Option[ColumnFamily[K, C]] = None,
                                     rowKeyMapperOpt: Option[E => Seq[K]] = None,
                                     columnNameMapperOpt: Option[E => Seq[C]] = None) {


  def withColumnFamily(columnFamily: ColumnFamily[K, C]): ViewBuilder[E, K, C] = {
    copy(columnFamilyOpt = Some(columnFamily))
  }

  def withRowKeyMapper(mapper: E => Seq[K]): ViewBuilder[E, K, C] = {
    copy(rowKeyMapperOpt = Some(mapper))
  }

  def withSingleRowKeyMapper(mapper: E => K): ViewBuilder[E, K, C] = {
    withRowKeyMapper(entity => Seq(mapper(entity)))
  }

  def withColumNameMapper(mapper: E => Seq[C]): ViewBuilder[E, K, C] = {
    copy(columnNameMapperOpt = Some(mapper))
  }

  def withSingleColumnNameMapper(mapper: E => C): ViewBuilder[E, K, C] = {
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

object ViewBuilder {

  def defaultTimeSeriesViewBuilder[E <: Entity[E]](keyspace: Keyspace, viewName: String): ViewBuilder[E, String, UUID] = {
    ViewBuilder[E, String, UUID](keyspace).withSingleRowKeyMapper(entity => viewName)
      .withSingleColumnNameMapper(entity => entity.uuid)
  }

  def defaultTextSeriesViewBuilder[E <: Entity[E]](keyspace: Keyspace, viewName: String): ViewBuilder[E, String, String] = {
    ViewBuilder[E, String, String](keyspace).withSingleRowKeyMapper(entity => viewName)
  }
}
