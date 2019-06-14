package ch.acmesoftware.arangodbscaladriver.querydsl

import org.scalatest.{FlatSpec, Matchers}

class QueryDslSpec extends FlatSpec with Matchers {

  "QueryDsl" should "build simple FOR-IN-FILTER queries" in {

    val test = FOR("c") IN "Characters" FILTER "c.name" `_==` "Frank" RETURN "c"

    test.toQuery shouldEqual "FOR c IN Characters FILTER c.name == Frank RETURN c"
  }
}
