import sbt.Keys.resolvers

name := "pact4s"

ThisBuild / version := "0.0.1"

ThisBuild / scalaVersion := "2.13.5"

val commonSettings = Seq(
  resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
)

lazy val pactSbt = (project in file("sbt-pact")).settings(
  name := "sbt-pact",
  sbtPlugin := true,
)

lazy val shared = (project in file("shared")).settings(
  name := "shared",
  libraryDependencies ++= Dependencies.shared,
)

lazy val munit =
  (project in file("munit-cats-effect-pact")).settings(commonSettings).settings(
    name := "munit-cats-effect-pact",
    libraryDependencies ++= Dependencies.munit,
    testFrameworks += new TestFramework("munit.Framework")
  ).dependsOn(shared)

lazy val scalaTest = (project in file("scalatest-pact")).settings(
  name := "scalatest-pact",
  libraryDependencies ++= Dependencies.scalatest,
).dependsOn(shared)

lazy val weaver = (project in file("weaver-pact")).settings(
  name := "weaver-pact",
  libraryDependencies ++= Dependencies.weaver,
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
).dependsOn(shared)

lazy val pact4s = (project in file(".")).aggregate(
  pactSbt, munit, scalaTest, weaver
)


