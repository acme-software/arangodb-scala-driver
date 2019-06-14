package ch.acmesoftware.arangodbscaladriver

import com.arangodb.entity.ErrorEntity

object Domain {

  case class ArangoError(error: ErrorEntity) extends Exception(s"""ArangoDB Error: ${error.getErrorMessage}""")
}
