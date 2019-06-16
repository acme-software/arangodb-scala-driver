package ch.acmesoftware.arangodbscaladriver

import cats.effect.Async
import cats.implicits._
import ch.acmesoftware.arangodbscaladriver.Domain.ArangoError
import com.arangodb.ArangoCollectionAsync
import com.arangodb.entity._
import com.arangodb.model.{CollectionPropertiesOptions, _}
import com.{arangodb => ar}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/** Scala API for collections, wrapping [[com.arangodb.ArangoCollectionAsync]]
  *
  * ==Document Management==
  *
  * {{{
  *   case class TestObj(_key: String, name: String)
  *
  *   implicit val codec = DocumentCodec.derive[testObj]
  *
  *   val collection: ArangoCollection[IO] = ???
  *
  *   for {
  *     _ <- collection.insertDocument(TestObj("1234", "Frank"))
  *
  *     ex1 <- collection.documentExists("1234") // true
  *
  *     _ <- collection.getDocument[String, TestObj]("1234")
  *
  *     _ <- collection.deleteDocument("1234")
  *
  *     ex2 <- collection.documentExists("1234") // false
  *   } yield ()
  * }}}
  *
  * @tparam F The effect type
  */
trait ArangoCollection[F[_]] {

  /** Access to underlying java driver's [[ar.ArangoCollectionAsync]] */
  def unwrap: ar.ArangoCollectionAsync

  /** Returns the parent database of this collection
    *
    * @return The wrapped [[ArangoDatabase]] which contains this collection
    */
  def db: ArangoDatabase[F]

  /** Returns the collections name */
  def name: String

  /** Inserts a document into the collection
    *
    * This method can also be used to overwrite already existing documents with the same `_key` field. See
    * [[com.arangodb.model.DocumentCreateOptions#overwrite]] javadoc for details.
    */
  def insertDocument[T](document: T,
                        createOptions: DocumentCreateOptions = new DocumentCreateOptions)
                       (implicit codec: DocumentCodec[T]): F[Unit]

  /** Batch insert documents into collection
    *
    * @see [[insertDocument]]
    */
  def insertDocuments[T](documents: Iterable[T],
                         createOptions: DocumentCreateOptions = new DocumentCreateOptions)
                        (implicit codec: DocumentCodec[T]): F[Unit]

  /** Gets a document by its `_key`
    *
    * @param key         The value of the documents `_key` property
    * @param readOptions See [[com.arangodb.model.DocumentReadOptions]] for details
    * @tparam K Type of the document key. If this is something other then `String`, [[AnyRef.toString]] will be used
    * @tparam T Type of the document
    */
  def getDocument[K, T](key: K,
                        readOptions: DocumentReadOptions = new DocumentReadOptions)
                       (implicit codec: DocumentCodec[T]): F[Option[T]]

  /** Batch read documents
    *
    * @see [[getDocument]]
    * @todo Streaming should be supported in a later version
    */
  def getDocuments[K, T](keys: Iterable[K],
                         readOptions: DocumentReadOptions = new DocumentReadOptions)
                        (implicit codec: DocumentCodec[T]): F[Iterable[Either[Throwable, T]]]

  /** Removed a document from the collection
    *
    * @param key           The value of the documents `_key` property
    * @param deleteOptions See [[com.arangodb.model.DocumentDeleteOptions]] for details
    * @param codec         Only needed if [[com.arangodb.model.DocumentDeleteOptions#returnOld]] is set to `true` for
    *                      deserializing the returned document.
    * @tparam T Only needed if [[com.arangodb.model.DocumentDeleteOptions#returnOld]] is set to `true` for
    *           deserializing the returned document.
    */
  def deleteDocument[T](key: String,
                        deleteOptions: DocumentDeleteOptions = new DocumentDeleteOptions)
                       (implicit codec: DocumentCodec[T]): F[DocumentDeleteEntity[T]]

  /** Batch delete documents
    *
    * @see [[deleteDocument]]
    */
  def deleteDocuments[T](keys: Iterable[String],
                         deleteOptions: DocumentDeleteOptions = new DocumentDeleteOptions)
                        (implicit codec: DocumentCodec[T]): F[Iterable[Either[Throwable, DocumentDeleteEntity[T]]]]

  /** Returns true if a document with given key exists on this collection
    *
    * @param key     The value of the documents `_key` property
    * @param options See [[com.arangodb.model.DocumentExistsOptions]] for details
    */
  def documentExists(key: String,
                     options: DocumentExistsOptions = new DocumentExistsOptions): F[Boolean]

  /** Returns details about an index */
  def getIndex(id: String): F[Option[IndexEntity]]

  /** Removes an index */
  def deleteIndex(id: String): F[Unit]

  /** Adds a new hash-index if no index with this name exists */
  def ensureHashIndex(fields: Iterable[String], options: HashIndexOptions): F[IndexEntity]

  /** Adds a new skiplist-index if no index with this name exists */
  def ensureSkiplistIndex(fields: Iterable[String], options: SkiplistIndexOptions): F[IndexEntity]

  /** Adds a new persistent-index if no index with this name exists */
  def ensurePersistentIndex(fields: Iterable[String], options: PersistentIndexOptions): F[IndexEntity]

  /** Adds a new geo-index if no index with this name exists */
  def ensureGeoIndex(fields: Iterable[String], options: GeoIndexOptions): F[IndexEntity]

  /** Adds a new fulltext-index if no index with this name exists */
  def ensureFulltextIndex(fields: Iterable[String], options: FulltextIndexOptions): F[IndexEntity]

  /** Returns a list of all indexes in this collection */
  def indexes: F[Iterable[IndexEntity]]

  /** Returns rue if this collection exists on the server
    *
    * @see [[create]]
    * @see [[drop]]
    */
  def exists: F[Boolean]

  /** Truncates (deletes all documents) the collection */
  def truncate: F[CollectionEntity]

  /** Returns the number of documents in the collection */
  def count: F[Long]

  /** Creates the collection
    *
    * @see [[exists]]
    * @see [[drop]]
    */
  def create(options: CollectionCreateOptions = new CollectionCreateOptions): F[CollectionEntity]

  /** Drops (deletes) the collection
    *
    * @see [[exists]]
    * @see [[create]]
    */
  def drop(isSystem: Boolean = false): F[Unit]

  /** Load the collection
    *
    * @see [[unload]]
    */
  def load: F[CollectionEntity]

  /** Unload the collection
    *
    * @see [[load]]
    */
  def unload: F[CollectionEntity]

  /** Returns information about the collection
    *
    * @see [[com.arangodb.entity.CollectionEntity]]
    */
  def info: F[CollectionEntity]

  /** Returns the collection's properties
    *
    * @see [[com.arangodb.entity.CollectionPropertiesEntity]]
    */
  def properties: F[CollectionPropertiesEntity]

  /** Changes the collection's properties to given ones
    *
    * @see [[com.arangodb.model.CollectionPropertiesOptions]]
    */
  def changeProperties(props: CollectionPropertiesOptions): F[CollectionPropertiesEntity]

  /** Changes the collection's name */
  def rename(newName: String): F[CollectionEntity]

  /** Returns the current revision
    *
    * @see [[com.arangodb.entity.CollectionRevisionEntity]]
    */
  def revision: F[CollectionRevisionEntity]

  /** Grants access to this collection */
  def grantAccess(user: String, permissions: Permissions): F[Unit]

  /** Revokes access to this collection */
  def revokeAccess(user: String): F[Unit]

  /** Resets access privileges to this collection to defaults */
  def resetAccess(user: String): F[Unit]

  /** Returns the permissions to this collection for the given user */
  def permissions(user: String): F[Permissions]
}

private[arangodbscaladriver] object ArangoCollection {

  def interpreter[F[_] : Async](wrapped: ar.ArangoCollectionAsync)
                               (implicit ec: ExecutionContext): ArangoCollection[F] = new ArangoCollection[F] {

    override def unwrap: ArangoCollectionAsync = wrapped

    override def db: ArangoDatabase[F] =
      ArangoDatabase.interpreter(wrapped.db())

    override def name: String =
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

    override def indexes: F[Iterable[IndexEntity]] =
      asyncF {
        wrapped.getIndexes
      }.map(_.asScala)

    override def exists: F[Boolean] =
      asyncF {
        wrapped.exists()
      }.map(Boolean.unbox)

    override def truncate: F[CollectionEntity] =
      asyncF {
        wrapped.truncate()
      }

    override def count: F[Long] =
      asyncF {
        wrapped.getProperties
      }.map(_.getCount)

    override def create(options: CollectionCreateOptions): F[CollectionEntity] =
      asyncF {
        wrapped.create(options)
      }

    override def drop(isSystem: Boolean): F[Unit] =
      discardedAsyncF {
        wrapped.drop(isSystem)
      }

    override def load: F[CollectionEntity] =
      asyncF(wrapped.load())

    override def unload: F[CollectionEntity] =
      asyncF(wrapped.unload())

    override def info: F[CollectionEntity] =
      asyncF(wrapped.getInfo)

    override def properties: F[CollectionPropertiesEntity] =
      asyncF(wrapped.getProperties)

    override def changeProperties(props: CollectionPropertiesOptions): F[CollectionPropertiesEntity] =
      asyncF(wrapped.changeProperties(props))

    override def rename(newName: String): F[CollectionEntity] =
      asyncF(wrapped.rename(newName))

    override def revision: F[CollectionRevisionEntity] =
      asyncF(wrapped.getRevision)

    override def grantAccess(user: String, permissions: Permissions): F[Unit] =
      discardedAsyncF(wrapped.grantAccess(user, permissions))

    override def revokeAccess(user: String): F[Unit] =
      discardedAsyncF(wrapped.revokeAccess(user))

    override def resetAccess(user: String): F[Unit] =
      discardedAsyncF(wrapped.resetAccess(user))

    override def permissions(user: String): F[Permissions] =
      asyncF(wrapped.getPermissions(user))
  }

}
