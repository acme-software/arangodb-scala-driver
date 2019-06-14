package ch.acmesoftware.arangodbscaladriver

package object querydsl {

  def FOR(ref: String): ForClause = ForClause(ref)

  trait QueryPart {
    def q: String
  }

  case class ForClause(ref: String) extends QueryPart {

    lazy val q = s"""FOR $ref"""

    def IN(collection: String): ForInClause = ForInClause(this, collection)
  }

  case class ForInClause(parent: QueryPart, collection: String) extends QueryPart {

    lazy val q = parent.q + s""" IN $collection"""

    def FILTER(fieldName: String): FilterClause = FilterClause(this, fieldName)
  }

  case class FilterClause(parent: QueryPart, fieldName: String) extends QueryPart {

    private lazy val _q = parent.q + s""" FILTER $fieldName"""

    override def q() = _q

    def eq(value: String): QueryClause = QueryClause(new QueryPart {
      override def q: String = _q + s""" == $value"""
    })

    def ne(value: String): QueryClause = QueryClause(new QueryPart {
      override def q: String = _q + s""" != $value"""
    })

    def `_==`(value: String) = eq(value)

    def `_!=`(value: String) = ne(value)
  }

  case class QueryClause(parent: QueryPart) extends QueryPart {
    private lazy val _q = parent.q

    override def q() = _q

    def RETURN(ref: String): QueryClause = QueryClause(new QueryPart {
      override def q: String = _q + s""" RETURN $ref"""
    })

    def toQuery(): String = parent.q
  }
}

