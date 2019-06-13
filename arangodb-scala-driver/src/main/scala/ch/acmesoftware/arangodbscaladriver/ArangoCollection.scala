package ch.acmesoftware.arangodbscaladriver

import cats.effect.Async
import ch.acmesoftware.arangodbscaladriver.Domain.ArangoError
import com.arangodb.entity.{DocumentDeleteEntity, ErrorEntity}
import com.arangodb.model.{DocumentCreateOptions, DocumentDeleteOptions, DocumentReadOptions, DocumentReplaceOptions}
import com.{arangodb => ar}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait ArangoCollection[F[_]] {

  def db(): ArangoDatabase[F]

  def name(): String

  def insertDocument[T](document: T,
                        createOptions: DocumentCreateOptions = new DocumentCreateOptions)
                       (implicit codec: DocumentCodec[T]): F[Unit]

  def insertDocuments[T](documents: Iterable[T],
                         createOptions: DocumentCreateOptions = new DocumentCreateOptions)
                        (implicit codec: DocumentCodec[T]): F[Unit]

  def getDocument[K, T](key: K,
                        readOptions: DocumentReadOptions = new DocumentReadOptions)
                       (implicit codec: DocumentCodec[T]): F[Option[T]]

  def getDocuments[K, T](keys: Iterable[K],
                         readOptions: DocumentReadOptions = new DocumentReadOptions)
                        (implicit codec: DocumentCodec[T]): F[Iterable[Either[Throwable, T]]]

  def deleteDocument[T](key: String,
                        deleteOptions: DocumentDeleteOptions = new DocumentDeleteOptions)
                       (implicit codec: DocumentCodec[T]): F[DocumentDeleteEntity[T]]

  def deleteDocuments[T](keys: Iterable[String],
                         deleteOptions: DocumentDeleteOptions = new DocumentDeleteOptions)
                        (implicit codec: DocumentCodec[T]): F[Iterable[Either[Throwable, DocumentDeleteEntity[T]]]]
}

private[arangodbscaladriver] object ArangoCollection {

  def interpreter[F[_] : Async](unwrap: ar.ArangoCollectionAsync)
                               (implicit ec: ExecutionContext): ArangoCollection[F] = new ArangoCollection[F] {

    override def db(): ArangoDatabase[F] =
      ArangoDatabase.interpreter(unwrap.db())

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

    override def insertDocuments[T](documents: Iterable[T], createOptions: DocumentCreateOptions)
                                   (implicit codec: DocumentCodec[T]): F[Unit] = Async[F].async { cb =>
      unwrap.insertDocuments(documents.map(codec.toJson).asJavaCollection, createOptions)
        .toScala
        .map(_ => ())
        .onComplete(e => cb(e.toEither))
    }

    override def getDocument[K, T](key: K,
                                   readOptions: DocumentReadOptions = new DocumentReadOptions)
                                  (implicit codec: DocumentCodec[T]): F[Option[T]] = Async[F].async { cb =>
      unwrap.getDocument(key.toString, classOf[String], readOptions)
        .toScala
        .map(Option.apply) // catch nulls
        .map(codec.fromJson(_).toTry)
        .onComplete(e => cb(e.flatten.toEither))
    }

    override def getDocuments[K, T](keys: Iterable[K],
                                    readOptions: DocumentReadOptions)
                                   (implicit codec: DocumentCodec[T]): F[Iterable[Either[Throwable, T]]] = Async[F].async { cb =>
      unwrap.getDocuments(keys.map(_.toString).asJavaCollection, classOf[String], readOptions)
        .toScala
        .map(_.getDocumentsAndErrors
          .asScala
          .map {
            case e: ErrorEntity => Left(ArangoError(e))
            case r: String => codec.fromJson(r)
          }
        ).onComplete(e => cb(e.toEither))
    }

    override def deleteDocument[T](key: String,
                                   deleteOptions: DocumentDeleteOptions)
                                  (implicit codec: DocumentCodec[T]): F[DocumentDeleteEntity[T]] = Async[F].async { cb =>
      unwrap.deleteDocument(key, classOf[String], deleteOptions)
        .toScala
        .onComplete(e => cb(e.toEither.flatMap(_.eitherT)))
    }

    override def deleteDocuments[T](keys: Iterable[String],
                                    deleteOptions: DocumentDeleteOptions)
                                   (implicit codec: DocumentCodec[T]): F[Iterable[Either[Throwable, DocumentDeleteEntity[T]]]] = Async[F].async { cb =>
      val x: Future[Iterable[Either[Throwable, DocumentDeleteEntity[T]]]] = unwrap.deleteDocuments(keys.map(_.toString).asJavaCollection, classOf[String], deleteOptions)
        .toScala
        .map(e => e.getDocumentsAndErrors.asScala.map {
          case e: ErrorEntity => Left(ArangoError(e))
          case r: DocumentDeleteEntity[String] => r.eitherT
        })
    }
  }

}
