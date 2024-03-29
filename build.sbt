lazy val commonSettings = Seq(
  organization := "ch.acmesoftware",
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq(
    "2.11.12",
    "2.12.8",
    "2.13.0"
  )
)

lazy val sharedDependencies = Seq(
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
  "org.typelevel" %% "cats-effect" % "1.2.0",
  //"org.typelevel" %% "mouse" % "0.21",
  "org.slf4j" % "slf4j-api" % "1.7.13",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test
)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  ).aggregate(`arangodb-scala-driver`, `arangodb-scala-driver-circe`)

lazy val `arangodb-scala-driver` = (project in file("arangodb-scala-driver"))
  .settings(
    commonSettings,
    libraryDependencies ++= sharedDependencies ++ Seq(
      "com.arangodb" % "arangodb-java-driver-async" % "5.0.4"
    )
  )

lazy val `arangodb-scala-driver-circe` = (project in file("arangodb-scala-driver-circe"))
  .settings(
    commonSettings,
    libraryDependencies ++= sharedDependencies ++ Seq(
      "io.circe" %% "circe-core" % "0.11.1",
      "io.circe" %% "circe-parser" % "0.11.1"
    )
  )
  .dependsOn(`arangodb-scala-driver`)

lazy val integrationtest = (project in file("integrationtest"))
  .settings(
    commonSettings,
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))),
    libraryDependencies ++= sharedDependencies
  )
  .dependsOn(`arangodb-scala-driver`, `arangodb-scala-driver-circe`)