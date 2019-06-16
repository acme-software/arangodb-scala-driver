package ch.acmesoftware.arangodbscaladriver

import cats.effect.Async
import cats.implicits._
import com.arangodb.entity._
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

  /** Returns true if the database exists
    *
    * @see [[create]]
    * @see [[drop]]
    */
  def exists: F[Boolean]

  /** Returns a list of all accessible datbases */
  def accessibleDatabases: F[Iterable[String]]

  /** Returns a collection (and creates it, if not exists) */
  def collection(name: String,
                 createOptions: Option[CollectionCreateOptions] = None): F[ArangoCollection[F]]

  /** Returns all collections */
  def collections: F[Iterable[CollectionEntity]]

  /** Returns an index for the given id */
  def index(id: String): F[Option[IndexEntity]]

  /** Deletes an index */
  def deleteIndex(id: String): F[Unit]

  /** Creates the database on server
    *
    * @see [[exists]]
    * @see [[drop]]
    */
  def create: F[Unit]

  /** Drops the database
    *
    * @see [[exists]]
    * @see [[create]]
    */
  def drop: F[Unit]

  /** Grants access to the database dbname for user user. You need permission to the _system database in order to
    * execute this call.
    */
  def grantAccess(user: String, permissions: Permissions = Permissions.RW): F[Unit]

  /** Revokes access to the database dbname for user user. You need permission to the _system database in order to
    * execute this call.
    */
  def revokeAccess(user: String): F[Unit] = grantAccess(user, Permissions.NONE)

  /** Clear the database access level, revert back to the default access level.
    */
  def resetAccess(user: String): F[Unit]

  /** Sets the default access level for collections within this database for the user user. You need
    * permission to the _system database in order to execute this call.
    */
  def grantDefaultCollectionAccess(user: String, permissions: Permissions): F[Unit]

  def permissions(user: String): F[Option[Permissions]]

  /** Returns db info
    *
    * @see com.arangodb.ArangoDatabase#getInfo()
    */
  def info: F[DatabaseEntity]


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

    override def index(id: String): F[Option[IndexEntity]] =
      asyncF(wrapped.getIndex(id))
        .map(Option.apply)

    override def deleteIndex(id: String): F[Unit] =
      discardedAsyncF(wrapped.deleteIndex(id))

    override def create: F[Unit] =
      discardedAsyncF(wrapped.create())

    override def drop: F[Unit] =
      discardedAsyncF(wrapped.drop())

    override def grantAccess(user: String, permissions: Permissions): F[Unit] =
      discardedAsyncF(wrapped.grantAccess(user, permissions))

    override def resetAccess(user: String): F[Unit] =
      discardedAsyncF(wrapped.resetAccess(user))

    override def grantDefaultCollectionAccess(user: String, permissions: Permissions): F[Unit] =
      discardedAsyncF(wrapped.grantDefaultCollectionAccess(user, permissions))

    override def permissions(user: String): F[Option[Permissions]] =
      asyncF(wrapped.getPermissions(user))
        .map(Option.apply)

    override def info: F[DatabaseEntity] =
      asyncF(wrapped.getInfo)

  }
}