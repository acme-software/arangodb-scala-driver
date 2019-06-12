package ch.acmesoftware

import ch.acmesoftware.arangodbscaladriver.DocumentCodec
import com.arangodb.entity.DocumentDeleteEntity

package object arangodbscaladriver {

  implicit class DocumentDeleteEntityOps(in: DocumentDeleteEntity[String]) {

    def eitherT[T](implicit codec: DocumentCodec[T]): Either[Throwable, DocumentDeleteEntity[T]] =
      codec.fromJson(in.getOld).map { old =>
        val e = new DocumentDeleteEntity[T]
        e.setOld(old)
        e
      }
  }

}
