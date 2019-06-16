package ch.acmesoftware.arangodbscaladriver

import com.arangodb.ArangoCursorAsync
import com.{arangodb => ar}

import scala.compat.java8.StreamConverters._
import scala.language.higherKinds

/** Scala wrapper type for [[ar.ArangoCursorAsync]]
  *
  * @tparam T The type of the result object
  */
trait ArangoCursor[T] {

  def unwrap: ar.ArangoCursorAsync[String]

  def streamRemaining: Stream[Either[Throwable, T]]
}

object ArangoCursor {

  def interpreter[T](wrapped: ar.ArangoCursorAsync[String])
                    (implicit codec: DocumentCodec[T]): ArangoCursor[T] = new ArangoCursor[T] {

    override def unwrap: ArangoCursorAsync[String] = wrapped

    override def streamRemaining: Stream[Either[Throwable, T]] =
      wrapped.streamRemaining().toScala[Stream]
        .map(codec.fromJson)
  }
}
