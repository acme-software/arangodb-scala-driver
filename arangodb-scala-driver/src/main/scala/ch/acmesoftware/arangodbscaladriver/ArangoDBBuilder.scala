package ch.acmesoftware.arangodbscaladriver

import cats.effect.{Async, Sync}
import com.{arangodb => ar}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

trait ArangoDBBuilder[F[_]] {

  /** Builds a new ArangoDB which basically represents a server connection
    *
    * @return Effect containing the ArangoDB representation
    */
  def build(host: String, port: Int): F[ArangoDB[F]]

  def build(host: String, port: Int, user: String, password: String): F[ArangoDB[F]]
}

object ArangoDBBuilder {

  def interpreter[F[_] : Async](implicit ec: ExecutionContext): ArangoDBBuilder[F] = new ArangoDBBuilder[F] {

    override def build(host: String, port: Int): F[ArangoDB[F]] = Async[F].delay {
      ArangoDB.interpreter(
        new ar.ArangoDBAsync.Builder()
          .host(host, port)
          .build()
      )
    }

    override def build(host: String, port: Int, user: String, password: String): F[ArangoDB[F]] = Async[F].delay {
      ArangoDB.interpreter(
        new ar.ArangoDBAsync.Builder()
          .host(host, port)
          .user(user)
          .password(password)
          .build()
      )
    }
  }

}
