package ch.acmesoftware.arangodbscaladriver.integrationtest

import cats.effect.IO
import ch.acmesoftware.arangodbscaladriver.ArangoDBBuilder
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class CollectionRoundtripSpec extends FlatSpec with Matchers {

  "Collections" should "be defined and be deletable again" in {
    val app = for {
      arango <- ArangoDBBuilder.interpreter[IO].build("127.0.0.1", 8529, "root", "root")
      db <- arango.db("test-db")
      dbName = db.name()
      collection <- db.collection("test-collection")
      colName = collection.name()
      _ <- db.drop()
      dbExistsAfterDrop <- arango.dbExists("test-db")
    } yield (dbName, colName, dbExistsAfterDrop)

    val res = app.unsafeRunSync()

    res._1 shouldEqual "test-db"
    res._2 shouldEqual "test-collection"
    res._3 shouldEqual false
  }
}
