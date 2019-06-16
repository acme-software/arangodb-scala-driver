package ch.acmesoftware.arangodbscaladriver

import cats.effect.IO
import ch.acmesoftware.arangodbscaladriver.TestHelpers._
import com.arangodb.{ArangoDBAsync, ArangoDatabaseAsync}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class ArangoDBSpec extends FlatSpec with Matchers with MockFactory {

  "ArangoDb" should "return existing DB" in {

    val arangoDatabaseMock = mock[ArangoDatabaseAsync]

    (arangoDatabaseMock.exists _).expects().returns(completableFutureOfJava(true))
    (arangoDatabaseMock.create _).expects().never()
    (arangoDatabaseMock.name _).expects().returns("test")

    val arangoDbMock = mock[ArangoDBAsync]

    (arangoDbMock.db(_: String)).expects("test").returns(arangoDatabaseMock)

    val sut = ArangoDB.interpreter[IO](arangoDbMock)

    val res = sut.db("test").unsafeRunSync()

    res.name shouldEqual "test"
  }

  it should "create DB if not exists" in {

    val arangoDatabaseMock = mock[ArangoDatabaseAsync]

    (arangoDatabaseMock.exists _).expects().returns(completableFutureOfJava(false))
    (arangoDatabaseMock.create _).expects().returns(completableFutureOfJava(true))
    (arangoDatabaseMock.name _).expects().returns("test")

    val arangoDbMock = mock[ArangoDBAsync]

    (arangoDbMock.db(_: String)).expects("test").returns(arangoDatabaseMock)

    val sut = ArangoDB.interpreter[IO](arangoDbMock)

    val res = sut.db("test").unsafeRunSync()

    res.name shouldEqual "test"
  }

  it should "provide access to underlying driver" in {

    val arangoDbMock = mock[ArangoDBAsync]
    val sut = ArangoDB.interpreter[IO](arangoDbMock)

    sut.unwrap shouldEqual arangoDbMock
  }
}
