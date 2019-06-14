package ch.acmesoftware

import java.util.concurrent.CompletableFuture

import cats.effect.Async
import com.arangodb.entity.DocumentDeleteEntity

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

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

  implicit class DocumentDeleteEntityOps(in: DocumentDeleteEntity[String]) {

    def eitherT[T](implicit codec: DocumentCodec[T]): Either[Throwable, DocumentDeleteEntity[T]] =
      codec.fromJson(in.getOld).map { old =>
        val e = new DocumentDeleteEntity[T]
        e.setOld(old)
        e
      }
  }

}
