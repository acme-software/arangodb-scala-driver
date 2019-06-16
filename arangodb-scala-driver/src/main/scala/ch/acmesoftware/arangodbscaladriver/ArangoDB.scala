package ch.acmesoftware.arangodbscaladriver

import cats.effect.Async
import com.arangodb.ArangoDBAsync
import com.{arangodb => ar}

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

/** Scala wrapper for [[ar.ArangoDBAsync]]
  *
  * @tparam F The effect type
  */
trait ArangoDB[F[_]] {

  /** Access to underlying java driver */
  def unwrap: ar.ArangoDBAsync

  /** Returns a database representation (and creates it if it does not exist)
    *
    * @param name The name of the database
    * @return The wrapped database
    */
  def db(name: String): F[ArangoDatabase[F]]

  def dbExists(name: String): F[Boolean]
}

private[arangodbscaladriver] object ArangoDB {

  def interpreter[F[_] : Async](wrapped: ar.ArangoDBAsync)
                               (implicit ec: ExecutionContext): ArangoDB[F] = new ArangoDB[F] {

    override def unwrap: ArangoDBAsync = wrapped

    override def db(name: String): F[ArangoDatabase[F]] = Async[F].async { cb =>

      val db = wrapped.db(name)

      val dbFuture = for {
        exists <- db.exists().toScala
        created <- if (!exists) db.create().toScala.map(_.booleanValue()) else Future.successful(true)
        db <- if (created) Future.successful(db) else Future.failed(new RuntimeException(s"Could not create db ${db.name()}"))
      } yield db

      dbFuture.onComplete(e => cb(e.toEither.map(ArangoDatabase.interpreter(_))))
    }

    override def dbExists(name: String): F[Boolean] = Async[F].async { cb =>
      wrapped.db(name).exists().toScala
        .map(Boolean.unbox)
        .onComplete(e => cb(e.toEither))
    }
  }
}