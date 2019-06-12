arangodb-scala-driver
=====================

[![Build Status](https://travis-ci.org/acme-software/arangodb-scala-driver.svg?branch=master)](https://travis-ci.org/acme-software/arangodb-scala-driver) 
![GitHub](https://img.shields.io/github/license/acme-software/arangodb-scala-driver.svg) 
[![Known Vulnerabilities](https://snyk.io//test/github/acme-software/arangodb-scala-driver/badge.svg?targetFile=build.sbt)](https://snyk.io//test/github/acme-software/arangodb-scala-driver?targetFile=build.sbt)

**Ideomatic [ArangoDB](https://www.arangodb.com) driver for [Scala](https://scala-lang.org). Right now, it's basically 
the [arangodb-driver-java-async](https://github.com/arangodb/arangodb-java-driver-async), wrapped into an tagless-final 
DSL (`F[_]`).**

*This library is under heavy development until v1.0.0 and therefore not ready for production!*

Example with `cats.effect.IO`
-----------------------------

```scala
import cats.effect._
import cats.implicits._
import ch.acmesoftware.arangodbscaladriver._

import scala.concurrent.ExecutionContext.Implicits.global

case class Test(_key: String, name: String)

object Test {
  // Use DocumentCodec.derive[T] from one of the supported json libs 
  // or implement a codec using DocumentCodec.of[T]
  implicit val codec: DocumentCodec[Test] = ???
}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = for {
    // connect to server
    arango <- ArangoDBBuilder.interpreter[IO].build("http://localhost", 8529)
    
    // get or create database instance
    db <- arango.db("myDb")
    
    // get or create collection
    collection <- db.collection("myCollection")
    
    // add document to collection
    _ <- collection.insertDocument(Test("key-1234", "Frank"))
    
    // delete document
    deleted <- collection.deleteDocument[Test]("key-1234")
    
    // output name of deleted document
    _ <- IO{ println(deleted.getOld.name) }
    
    res <- IO.pure(ExitCode.Success)
  } yield res
  
}
```

About
-----

The complete library is designed as a tagless final DSL. Therefore, the Effect type (`F[_]`) can be virtually everything
which satisfies `cats.effect.Sync[F]`. In the examples, `cats.effect.IO` is used.

Beside that, one key point during API design was to keep is as similar as possible to 
[arangodb-driver-java](https://github.com/arangodb/arangodb-java-driver). This simplifies documentation, usability and 
the upgrad path for future ArangoDB versions.

In the examples, the global Scala `ExecuritionContext` is used. For production purposes, please create your own and pass 
it either implicitly or explicit.

Installation
------------

Add dependencies to SBT build:

```scala
libraryDependencies ++= Seq(
  "ch.acmesoftware" %% "arangodb-scala-driver" % arangoVersion,
  "ch.acmesoftware" %% "arangodb-scala-driver-circe" % arangoVersion
  //"ch.acmesoftware" %% "arangodb-scala-driver-playjson" % arangoVersion
)
```
 
Usage
-----

### JSON & Codecs

A `DocumentCodec` is needed to serialize/deserialize JSON, which is in turn used by ArangoDB. You can provide an 
implicit `DocumentCodec` in two different ways:

**By using one of the supported JSON libs**

You can just use e.g. [circe]() by adding the appropriate dependency:

```scala
libraryDependencies ++= Seq(
  // ...
  "ch.acmesoftware" %% "arangodb-scala-driver-circe" % arangoVersion
)
```

Add the imports:

```scala
import ch.acmesoftware.arangodbscaladriver._

// circe support
import ch.acmesoftware.arangodbscaladriver.circe

implicit val codec = DocumentCodec.derive[Test]

```

**By implementing it (not recommended)**

```scala
import ch.acmesoftware.arangodbscaladriver._

case class Test(name: String)

DocumentCodec.of[Test](
  e => s"""{ name: "${e.name}" }""", 
  str => Left(new RuntimeException("Not deserializable"))
)
```

Disclaimer
----------

This library is released under the terms of the [MIT License](LICENSE.txt) by 
[ACME Software Solutions GmbH](https://www.acmesoftware.ch). There is no legal relationship to ArangoDB Inc., 
ArangoDB GmbH, nor any of their employees. Please visit [arangodb.com](https://www.arangodb.com) for more information.