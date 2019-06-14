package ch.acmesoftware.arangodbscaladriver

import cats.effect.IO
import com.arangodb.{ArangoCollectionAsync, ArangoDatabaseAsync}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class ArangoCollectionSpec extends FlatSpec with Matchers with MockFactory {

  "ArangoCollection" should "provide access to underlying java driver" in {
    val arangoCollectionMock = mock[ArangoCollectionAsync]
    val sut = ArangoCollection.interpreter[IO](arangoCollectionMock)

    sut.unwrap shouldEqual arangoCollectionMock
  }
}
