package ch.acmesoftware.arangodbscaladriver

import cats.effect.Async
import cats.implicits._
import ch.acmesoftware.arangodbscaladriver.Domain.ArangoError
import com.arangodb.ArangoCollectionAsync
import com.arangodb.entity.{DocumentDeleteEntity, ErrorEntity, IndexEntity}
import com.arangodb.model._
import com.{arangodb => ar}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

trait ArangoCollection[F[_]] {

  /** Access to underlying java driver */
  def unwrap: ar.ArangoCollectionAsync

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

  def documentExists(key: String,
                     options: DocumentExistsOptions = new DocumentExistsOptions): F[Boolean]

  def getIndex(id: String): F[Option[IndexEntity]]

  def deleteIndex(id: String): F[Unit]

  def ensureHashIndex(fields: Iterable[String], options: HashIndexOptions): F[IndexEntity]

  def ensureSkiplistIndex(fields: Iterable[String], options: SkiplistIndexOptions): F[IndexEntity]

  def ensurePersistentIndex(fields: Iterable[String], options: PersistentIndexOptions): F[IndexEntity]

  def ensureGeoIndex(fields: Iterable[String], options: GeoIndexOptions): F[IndexEntity]

  def ensureFulltextIndex(fields: Iterable[String], options: FulltextIndexOptions): F[IndexEntity]
}

private[arangodbscaladriver] object ArangoCollection {

  def interpreter[F[_] : Async](wrapped: ar.ArangoCollectionAsync)
                               (implicit ec: ExecutionContext): ArangoCollection[F] = new ArangoCollection[F] {

    override def unwrap: ArangoCollectionAsync = wrapped

    override def db(): ArangoDatabase[F] =
      ArangoDatabase.interpreter(wrapped.db())

    override def name(): String =
      wrapped.name()

    override def insertDocument[T](document: T,
                                   createOptions: DocumentCreateOptions)
                                  (implicit codec: DocumentCodec[T]): F[Unit] =
      discardedAsyncF {
        wrapped.insertDocument(codec.toJson(document), createOptions)
      }

    override def insertDocuments[T](documents: Iterable[T], createOptions: DocumentCreateOptions)
                                   (implicit codec: DocumentCodec[T]): F[Unit] =
      discardedAsyncF {
        wrapped.insertDocuments(documents.map(codec.toJson).asJavaCollection, createOptions)
      }

    override def getDocument[K, T](key: K,
                                   readOptions: DocumentReadOptions = new DocumentReadOptions)
                                  (implicit codec: DocumentCodec[T]): F[Option[T]] =
      asyncF[F, String] {
        wrapped.getDocument(key.toString, classOf[String], readOptions)
      }
        .map(Option.apply)
        .map {
          case Some(e) => codec.fromJson(e).map(_.some)
          case None => Right(None)
        }
        .flatMap(_.liftTo[F])

    override def getDocuments[K, T](keys: Iterable[K],
                                    readOptions: DocumentReadOptions)
                                   (implicit codec: DocumentCodec[T]): F[Iterable[Either[Throwable, T]]] =
      asyncF {
        wrapped.getDocuments(keys.map(_.toString).asJavaCollection, classOf[String], readOptions)
      }.map(_.getDocumentsAndErrors
        .asScala
        .map {
          case e: ErrorEntity => Left(ArangoError(e))
          case r: String => codec.fromJson(r)
        }
      )

    override def deleteDocument[T](key: String,
                                   deleteOptions: DocumentDeleteOptions)
                                  (implicit codec: DocumentCodec[T]): F[DocumentDeleteEntity[T]] =
      asyncF[F, DocumentDeleteEntity[String]] {
        wrapped.deleteDocument(key, classOf[String], deleteOptions)
      }.flatMap(_.eitherT[T].liftTo[F])

    override def deleteDocuments[T](keys: Iterable[String],
                                    deleteOptions: DocumentDeleteOptions)
                                   (implicit codec: DocumentCodec[T]): F[Iterable[Either[Throwable, DocumentDeleteEntity[T]]]] =
      asyncF {
        wrapped.deleteDocuments(keys.map(_.toString).asJavaCollection, classOf[String], deleteOptions)
      }.map(e => e.getDocumentsAndErrors.asScala.map {
        case e: ErrorEntity => Left(ArangoError(e))
        case r: DocumentDeleteEntity[String] => r.eitherT
      })

    override def documentExists(key: String,
                                options: DocumentExistsOptions): F[Boolean] =
      asyncF {
        wrapped.documentExists(key, options)
      }.map(Boolean.unbox)

    override def getIndex(id: String): F[Option[IndexEntity]] =
      asyncF[F, IndexEntity] {
        wrapped.getIndex(id)
      }.map(Option.apply)

    override def deleteIndex(id: String): F[Unit] =
      discardedAsyncF {
        wrapped.deleteIndex(id)
      }

    override def ensureHashIndex(fields: Iterable[String], options: HashIndexOptions): F[IndexEntity] =
      asyncF {
        wrapped.ensureHashIndex(fields.asJava, options)
      }

    override def ensureSkiplistIndex(fields: Iterable[String], options: SkiplistIndexOptions): F[IndexEntity] =
      asyncF {
        wrapped.ensureSkiplistIndex(fields.asJava, options)
      }

    override def ensurePersistentIndex(fields: Iterable[String], options: PersistentIndexOptions): F[IndexEntity] =
      asyncF {
        wrapped.ensurePersistentIndex(fields.asJava, options)
      }

    override def ensureGeoIndex(fields: Iterable[String], options: GeoIndexOptions): F[IndexEntity] =
      asyncF {
        wrapped.ensureGeoIndex(fields.asJava, options)
      }

    override def ensureFulltextIndex(fields: Iterable[String], options: FulltextIndexOptions): F[IndexEntity] =
      asyncF {
        wrapped.ensureFulltextIndex(fields.asJava, options)
      }
  }

}
