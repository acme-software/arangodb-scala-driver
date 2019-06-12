package ch.acmesoftware.arangodbscaladriver

import ch.acmesoftware.arangodbscaladriver.DocumentCodec.CodecBuilder
import io.circe._
import io.circe.parser._
import io.circe.syntax._

package object circe {

  implicit def circeDocumentCodecBuilder[T](implicit enc: Encoder[T], dec: Decoder[T]): CodecBuilder[T] =
    () => DocumentCodec.of[T](
      _.asJson.spaces4,
      decode[T]
    )
}
