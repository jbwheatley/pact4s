import sbt.Keys.{crossScalaVersions, resolvers}

val scala213 = "2.13.5"
val scala3 = "3.0.0-RC2"

inThisBuild(List(
  name := "pact4s",
  organization := "io.github.jbwheatley",
  homepage := Some(url("https://github.com/jbwheatley/pact4s")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "jbwheatley",
      "Jack Wheatley",
      "jackbwheatley@gmail.com",
      url("https://github.com/jbwheatley")
    )
  ),
  crossScalaVersions := List(scala213, scala3),
))


publish/skip := true // don't publish the root project

val commonSettings = Seq(
  resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns),
)

//lazy val pactSbt = (project in file("sbt-pact")).settings(
//  name := "sbt-pact",
//  sbtPlugin := true,
//)

lazy val shared = (project in file("shared"))
  .settings(commonSettings)
  .settings(
    name := "shared",
    libraryDependencies ++= Dependencies.shared,
    publish/skip := true
  )

lazy val munit =
  (project in file("munit-cats-effect-pact"))
    .settings(commonSettings)
    .settings(
    name := "pact4s-munit-cats-effect",
    libraryDependencies ++= Dependencies.munit,
    testFrameworks += new TestFramework("munit.Framework")
  ).dependsOn(shared)

lazy val scalaTest = (project in file("scalatest-pact")).settings(commonSettings)
  .settings(
  name := "pact4s-scalatest",
  libraryDependencies ++= Dependencies.scalatest,
).dependsOn(shared)

lazy val weaver = (project in file("weaver-pact")).settings(commonSettings)
  .settings(commonSettings)
  .settings(
  name := "pact4s-weaver",
  libraryDependencies ++= Dependencies.weaver,
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
).dependsOn(shared)

lazy val pact4s = (project in file(".")).aggregate(
  munit, scalaTest, weaver
)


