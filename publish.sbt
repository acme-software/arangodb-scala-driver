ThisBuild / organization := "ch.acmesoftware"
ThisBuild / organizationName := "ACME Software SOlutions GmbH"
ThisBuild / organizationHomepage := Some(url("https://www.acmesoftware.ch"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/acme-software/arangodb-scala-driver"),
    "scm:git@github.com:acme-software/arangodb-scala-driver.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "frne",
    name  = "Frank Neff",
    email = "me@franks.codes",
    url   = url("http://franks.codes")
  )
)

ThisBuild / description := "Idiomatic Scala Driver for ArangoDB"
ThisBuild / licenses := List("MIT" -> new URL("https://github.com/acme-software/arangodb-scala-driver/blob/master/LICENSE.txt"))
ThisBuild / homepage := Some(url("https://acmesoftware.ch/arangodb-scala-driver/"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
