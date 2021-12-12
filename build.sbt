import sbt.Keys.{resolvers, testFrameworks}

import scala.util.Try

val scala212       = "2.12.15"
val scala213       = "2.13.7"
val scala2Versions = Seq(scala212, scala213)
val scala3         = "3.0.0-RC2"

sonatypeCredentialHost := Sonatype.sonatype01

inThisBuild(
  List(
    organization := "io.github.jbwheatley",
    homepage     := Some(url("https://github.com/jbwheatley/pact4s")),
    developers := List(
      Developer(
        "jbwheatley",
        "jbwheatley",
        "jbwheatley@proton.me",
        url("https://github.com/jbwheatley")
      )
    ),
    startYear    := Some(2021),
    licenses     := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := scala213,
    crossScalaVersions := scala2Versions
  )
)

publish / skip := true // don't publish the root project

val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  )
)

val moduleBase =
  Def.setting((Compile / scalaSource).value.getParentFile.getParentFile.getParentFile)

lazy val shared =
  (project in file("shared")).settings(commonSettings)
    .settings(
      name := "pact4s-core-java8",
      libraryDependencies ++= Dependencies.shared
    )

lazy val circe =
  (project in file("circe")).settings(commonSettings)
    .settings(
      name := "pact4s-circe-java8",
      libraryDependencies ++= Dependencies.circe,
    )
    .dependsOn(shared)

lazy val playJson =
  (project in file("play-json")).settings(commonSettings)
    .settings(
      name := "pact4s-play-json-java8",
      libraryDependencies ++= Dependencies.playJson
    )
    .dependsOn(shared)

lazy val munit =
  (project in file("munit-cats-effect-pact")).settings(commonSettings)
    .settings(
      name := "pact4s-munit-cats-effect-java8",
      libraryDependencies ++= Dependencies.munit
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val scalaTest =
  (project in file("scalatest-pact")).settings(commonSettings)
    .settings(
      name := "pact4s-scalatest-java8",
      libraryDependencies ++= Dependencies.scalatest
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val weaver =
  (project in file("weaver-pact"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-weaver-java8",
      libraryDependencies ++= Dependencies.weaver,
      testFrameworks ++= {
        if (Try(System.getenv("TEST_WEAVER").toBoolean).getOrElse(true))
          Seq(new TestFramework("weaver.framework.CatsEffect"))
        else Nil
      }
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val pact4s = (project in file("."))
  .settings(commonSettings)
  .enablePlugins(AutomateHeaderPlugin)
  .aggregate(
    munit,
    scalaTest,
    weaver,
    shared,
    circe,
    playJson
  )

addCommandAlias(
  "commitCheck",
  List(
    "scalafmtCheck",
    "headerCheck",
    "+compile:doc",
    "+test"
  )
    .mkString(";", ";", "")
)
