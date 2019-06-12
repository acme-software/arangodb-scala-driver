package ch.acmesoftware.arangodbscaladriver

import cats.effect.Async
import com.arangodb.entity.DocumentDeleteEntity
import com.arangodb.model.{DocumentCreateOptions, DocumentDeleteOptions}
import com.{arangodb => ar}

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

trait ArangoCollection[F[_]] {

  def name(): String

  def insertDocument[T](document: T,
                        createOptions: DocumentCreateOptions = new DocumentCreateOptions)
                       (implicit codec: DocumentCodec[T]): F[Unit]

  def deleteDocument[T](key: String,
                        deleteOptions: DocumentDeleteOptions = new DocumentDeleteOptions)
                       (implicit codec: DocumentCodec[T]): F[DocumentDeleteEntity[T]]
}

private[arangodbscaladriver] object ArangoCollection {

  def interpreter[F[_] : Async](unwrap: ar.ArangoCollectionAsync)
                               (implicit ec: ExecutionContext): ArangoCollection[F] = new ArangoCollection[F] {

    override def name(): String =
      unwrap.name()

    override def insertDocument[T](document: T,
                                   createOptions: DocumentCreateOptions)
                                  (implicit codec: DocumentCodec[T]): F[Unit] = Async[F].async { cb =>
      unwrap.insertDocument(codec.toJson(document), createOptions)
        .toScala
        .map(_ => ())
        .onComplete(e => cb(e.toEither))
    }

    override def deleteDocument[T](key: String,
                                   deleteOptions: DocumentDeleteOptions)
                                  (implicit codec: DocumentCodec[T]): F[DocumentDeleteEntity[T]] = Async[F].async { cb =>
      unwrap.deleteDocument(key, classOf[String], deleteOptions)
        .toScala
        .onComplete(e => cb(e.toEither.flatMap(_.eitherT)))
    }
  }

}
