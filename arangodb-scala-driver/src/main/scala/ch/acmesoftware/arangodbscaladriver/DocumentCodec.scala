package ch.acmesoftware.arangodbscaladriver

import cats.implicits._

trait DocumentCodec[T] {

  def toJson(doc: T): String

  def fromJson(str: String): Either[Throwable, T]

  def fromJson(strOpt: Option[String]): Either[Throwable, Option[T]] = strOpt match {
    case Some(str) => fromJson(str).map(_.some)
    case None => Right(None)
  }
}

object DocumentCodec {

  type CodecBuilder[T] = () => DocumentCodec[T]

  def of[T](encFn: T => String, decFn: String => Either[Throwable, T]): DocumentCodec[T] = new DocumentCodec[T] {

    override def toJson(doc: T): String = encFn(doc)

    override def fromJson(str: String): Either[Throwable, T] = decFn(str)
  }

  def derive[T](implicit builder: CodecBuilder[T]): DocumentCodec[T] = builder()
}