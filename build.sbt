import sbt.Keys.{crossScalaVersions, resolvers}

val scala213 = "2.13.5"
val scala3   = "3.0.0-RC2"

sonatypeCredentialHost := Sonatype.sonatype01

inThisBuild(
  List(
    organization := "io.github.jbwheatley",
    homepage := Some(url("https://github.com/jbwheatley/pact4s")),
    developers := List(
      Developer(
        "jbwheatley",
        "jbwheatley",
        "jbwheatley@proton.me",
        url("https://github.com/jbwheatley")
      )
    ),
    scalaVersion := scala213,
    crossScalaVersions := List(
      scala213
    ) //scala 3 support tmp removed due to https://github.com/lampepfl/dotty/issues/12086
  )
)

publish / skip := true // don't publish the root project

val commonSettings = Seq(
  resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(
    Resolver.ivyStylePatterns
  ),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  startYear := Some(2021),
  headerLicense := Some(
    HeaderLicense.ALv2(
      s"${startYear.value.get}-${java.time.Year.now}",
      organization.value
    )
  )
)

lazy val shared = (project in file("shared"))
  .settings(commonSettings)
  .settings(
    name := "shared",
    libraryDependencies ++= Dependencies.shared
  )

lazy val circe = (project in file("circe"))
  .settings(commonSettings)
  .settings(
    name := "pact4s-circe",
    libraryDependencies ++= Dependencies.circe
  )
  .dependsOn(shared)

lazy val munit =
  (project in file("munit-cats-effect-pact"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-munit-cats-effect",
      libraryDependencies ++= Dependencies.munit,
      testFrameworks += new TestFramework("munit.Framework")
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val scalaTest = (project in file("scalatest-pact"))
  .settings(commonSettings)
  .settings(
    name := "pact4s-scalatest",
    libraryDependencies ++= Dependencies.scalatest
  )
  .dependsOn(shared % "compile->compile;test->test")
  .dependsOn(circe % "test->test")

lazy val weaver = (project in file("weaver-pact"))
  .settings(commonSettings)
  .settings(commonSettings)
  .settings(
    name := "pact4s-weaver",
    libraryDependencies ++= Dependencies.weaver,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
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
    circe
  )
