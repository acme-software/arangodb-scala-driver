package ch.acmesoftware.arangodbscaladriver

trait DocumentCodec[T] {

  def toJson(doc: T): String

  def fromJson(str: String): Either[Throwable, T]

}

object DocumentCodec {

  type CodecBuilder[T] = () => DocumentCodec[T]

  def of[T](encFn: T => String, decFn: String => Either[Throwable, T]): DocumentCodec[T] = new DocumentCodec[T] {

    override def toJson(doc: T): String = encFn(doc)

    override def fromJson(str: String): Either[Throwable, T] = decFn(str)
  }

  def derive[T](implicit builder: CodecBuilder[T]): DocumentCodec[T] = builder()
}