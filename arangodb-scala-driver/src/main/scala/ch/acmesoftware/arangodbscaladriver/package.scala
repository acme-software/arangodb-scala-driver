package ch.acmesoftware

import java.util.concurrent.CompletableFuture

import cats.implicits._
import cats.effect.Async
import com.arangodb.entity.DocumentDeleteEntity

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext
import scala.language.higherKinds
import scala.util.{Either, Failure, Right, Success, Try}

package object arangodbscaladriver {

  def asyncF[F[_] : Async, T](f: => CompletableFuture[T])
                             (implicit ec: ExecutionContext): F[T] =
    Async[F].async { cb =>
      f.toScala.onComplete(e => cb(e.toEither))
    }

  def discardedAsyncF[F[_] : Async](f: => CompletableFuture[_])
                                   (implicit ec: ExecutionContext): F[Unit] =
    Async[F].async { cb =>
      f.toScala
        .map(_ => ())
        .onComplete(e => cb(e.toEither))
    }

  def valueMapAnyToJava(in: Map[String, Any]): java.util.Map[java.lang.String, java.lang.Object] =
    in.mapValues(_.asInstanceOf[AnyRef]).asJava

  implicit class DocumentDeleteEntityOps(in: DocumentDeleteEntity[String]) {

    def eitherT[T](implicit codec: DocumentCodec[T]): Either[Throwable, DocumentDeleteEntity[T]] =
      codec.fromJson(in.getOld).map { old =>
        val e = new DocumentDeleteEntity[T]
        e.setOld(old)
        e
      }
  }

  implicit class Scala211TryOps[T](in: Try[T]) {

    /** Scala 2.11 compat */
    def toEither: Either[Throwable, T] = in match {
      case Success(value) => Right(value)
      case Failure(exception) => Left(exception)
    }
  }
}
