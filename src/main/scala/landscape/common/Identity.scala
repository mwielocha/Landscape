package landscape.common

import scala.Option

/**
 * author mikwie
 *
 */
trait Equal[A] {

  def isEqual(left: A, right: A): Boolean

}

trait Identity[T] {

  def value: T

  def ===(other: T)(implicit e: Equal[T]): Boolean = {
    e.isEqual(value, other)
  }
  def !==(other: T)(implicit e: Equal[T]): Boolean = {
    !(this === other)
  }
}

object DefaultIdentityImplicits {

  implicit def optionEqual[A] = new Equal[Option[A]] {
    def isEqual(left: Option[A], right: Option[A]): Boolean = left == right
  }

  implicit class OptionIdentity[T](val value: Option[T]) extends Identity[Option[T]]

  implicit def defaultEqual[X] = new Equal[X] {
    def isEqual(left: X, right: X): Boolean = left == right
  }

  implicit class DefaultIdentity[X](val value: X) extends Identity[X]

}
