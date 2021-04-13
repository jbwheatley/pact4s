import sbt.Keys.{crossScalaVersions, resolvers}

name := "pact4s"

inThisBuild(List(
  organization := "io.github.jbwheatley",
  homepage := Some(url("https://github.com/jbwheatley/pact4s")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
))

val scala213 = "2.13.5"
val scala3 = "3.0.0-RC2"

ThisBuild / scalaVersion := scala213

val commonSettings = Seq(
  resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns),
  crossScalaVersions ++= Seq(scala213, scala3)
)

lazy val pactSbt = (project in file("sbt-pact")).settings(
  name := "sbt-pact",
  sbtPlugin := true,
)

lazy val shared = (project in file("shared"))
  .settings(commonSettings)
  .settings(
    name := "shared",
    libraryDependencies ++= Dependencies.shared,
  )

lazy val munit =
  (project in file("munit-cats-effect-pact"))
    .settings(commonSettings)
    .settings(
    name := "munit-cats-effect-pact",
    libraryDependencies ++= Dependencies.munit,
    testFrameworks += new TestFramework("munit.Framework")
  ).dependsOn(shared)

lazy val scalaTest = (project in file("scalatest-pact")).settings(commonSettings)
  .settings(
  name := "scalatest-pact",
  libraryDependencies ++= Dependencies.scalatest,
).dependsOn(shared)

lazy val weaver = (project in file("weaver-pact")).settings(commonSettings)
  .settings(commonSettings)
  .settings(
  name := "weaver-pact",
  libraryDependencies ++= Dependencies.weaver,
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
).dependsOn(shared)

lazy val pact4s = (project in file(".")).aggregate(
  munit, scalaTest, weaver
)


