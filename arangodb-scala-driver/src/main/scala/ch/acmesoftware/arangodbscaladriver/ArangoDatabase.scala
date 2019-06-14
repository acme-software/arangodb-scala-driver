package ch.acmesoftware.arangodbscaladriver

import cats.effect.Async
import com.arangodb.{ArangoCollectionAsync, ArangoDatabaseAsync}
import com.arangodb.entity.DatabaseEntity
import com.arangodb.model.CollectionCreateOptions
import com.{arangodb => ar}

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait ArangoDatabase[F[_]] {

  /** Access to underlying java driver */
  def unwrap: ar.ArangoDatabaseAsync

  /** Returns db name
    *
    * @see com.arangodb.ArangoDatabase#name()
    */
  def name(): String

  /** Returns db info
    *
    * @see com.arangodb.ArangoDatabase#getInfo()
    */
  def info(): F[DatabaseEntity]

  /** Drops the database
    *
    * @see com.arangodb.ArangoDatabase#drop()
    */
  def drop(): F[Unit]

  /** Returns a collection (and creates it, if not exists) */
  def collection(name: String,
                 createOptions: Option[CollectionCreateOptions] = None): F[ArangoCollection[F]]
}

private[arangodbscaladriver] object ArangoDatabase {

  def interpreter[F[_] : Async](wrapped: ar.ArangoDatabaseAsync)
                               (implicit ec: ExecutionContext): ArangoDatabase[F] = new ArangoDatabase[F] {

    override def unwrap: ArangoDatabaseAsync = wrapped

    override def name(): String =
      wrapped.name()

    override def info(): F[DatabaseEntity] = Async[F].async { cb =>
      wrapped.getInfo.toScala.onComplete(e => cb(e.toEither))
    }

    override def drop(): F[Unit] = Async[F].async { cb =>
      wrapped.drop().toScala
        .map(assert(_, s"Could not drop database ${wrapped.name()}"))
        .onComplete(e => cb(e.toEither))
    }

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
  }
}