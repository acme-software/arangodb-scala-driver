package ch.acmesoftware.arangodbscaladriver

import cats.effect.IO
import com.arangodb.{ArangoDBAsync, ArangoDatabaseAsync}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class ArangoDatabaseSpec extends FlatSpec with Matchers with MockFactory {

  "ArangoDatabase" should "provide access to underlying java driver" in {
    val arangoDatabaseMock = mock[ArangoDatabaseAsync]
    val sut = ArangoDatabase.interpreter[IO](arangoDatabaseMock)

    sut.unwrap shouldEqual arangoDatabaseMock
  }
}
