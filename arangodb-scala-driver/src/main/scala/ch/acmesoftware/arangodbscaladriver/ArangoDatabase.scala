package ch.acmesoftware.arangodbscaladriver

import cats.effect.Async
import cats.implicits._
import com.arangodb.entity.{ArangoDBVersion, CollectionEntity, DatabaseEntity}
import com.arangodb.model.CollectionCreateOptions
import com.arangodb.{ArangoCollectionAsync, ArangoDatabaseAsync}
import com.{arangodb => ar}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait ArangoDatabase[F[_]] {

  /** Access to underlying java driver */
  def unwrap: ar.ArangoDatabaseAsync

  /** Returns the arangodb server ref on which this db runs */
  def arango: ArangoDB[F]

  /** Returns db name
    *
    * @see com.arangodb.ArangoDatabase#name()
    */
  def name: String

  /** Returns arango version details */
  def version: F[ArangoDBVersion]

  /** Returns true if the database exists */
  def exists: F[Boolean]

  /** Returns a list of all accessible datbases */
  def accessibleDatabases: F[Iterable[String]]

  /** Returns a collection (and creates it, if not exists) */
  def collection(name: String,
                 createOptions: Option[CollectionCreateOptions] = None): F[ArangoCollection[F]]

  def collections: F[Iterable[CollectionEntity]]

  /** Returns db info
    *
    * @see com.arangodb.ArangoDatabase#getInfo()
    */
  def info: F[DatabaseEntity]

  /** Drops the database
    *
    * @see com.arangodb.ArangoDatabase#drop()
    */
  def drop: F[Unit]


}

private[arangodbscaladriver] object ArangoDatabase {

  def interpreter[F[_] : Async](wrapped: ar.ArangoDatabaseAsync)
                               (implicit ec: ExecutionContext): ArangoDatabase[F] = new ArangoDatabase[F] {

    override def unwrap: ArangoDatabaseAsync = wrapped

    override def arango: ArangoDB[F] =
      ArangoDB.interpreter(wrapped.arango())

    override def name: String =
      wrapped.name()

    override def version: F[ArangoDBVersion] =
      asyncF(wrapped.getVersion)

    override def exists: F[Boolean] =
      asyncF(wrapped.exists()).map(Boolean.unbox)

    override def accessibleDatabases: F[Iterable[String]] =
      asyncF(wrapped.getAccessibleDatabases)
        .map(_.asScala)

    override def collection(name: String,
                            createOptions: Option[CollectionCreateOptions] = None): F[ArangoCollection[F]] =
      Async[F].async { cb =>
        val c: ArangoCollectionAsync = wrapped.collection(name)

        val created: Future[ArangoCollection[F]] = for {
          exists <- c.exists().toScala
          _ <- if (!exists) {
            c.create(createOptions.getOrElse(new CollectionCreateOptions)).toScala
              .map(_ => true)
          } else {
            Future.successful(false)
          }
          res <- Future.successful(ArangoCollection.interpreter[F](c))
        } yield res

        created.onComplete(e => cb(e.toEither))
      }

    override def collections: F[Iterable[CollectionEntity]] =
      asyncF(wrapped.getCollections)
        .map(_.asScala)

    override def info: F[DatabaseEntity] =
      asyncF(wrapped.getInfo)

    override def drop: F[Unit] =
      discardedAsyncF(wrapped.drop())


  }
}