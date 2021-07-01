import sbt.Keys.{crossScalaVersions, resolvers, testFrameworks}
import sbt.Test

val scala212       = "2.12.14"
val scala213       = "2.13.6"
val scala2Versions = Seq(scala212, scala213)
val scala3         = "3.0.0-RC2"

sonatypeCredentialHost := Sonatype.sonatype01

inThisBuild(
  List(
    organization := "io.github.jbwheatley",
    homepage := Some(url("https://github.com/jbwheatley/pact4s")),
    developers := List(
      Developer(
        "jbwheatley",
        "Jack Wheatley",
        "jackbwheatley@gmail.com",
        url("https://github.com/jbwheatley")
      )
    ),
    scalaVersion := scala213,
    crossScalaVersions := List(
      scala212,
      scala213
    ), //scala 3 support tmp removed due to https://github.com/lampepfl/dotty/issues/12086
    Test / parallelExecution := false
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

def moduleName(base: String, axis: Seq[VirtualAxis]): String = {
  val series = axis.collectFirst { case c: PactJvmAxis => c.series }.get
  s"$base$series"
}

lazy val shared =
  (projectMatrix in file("shared"))
    .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java11), identity(_))
    .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java8), identity(_))
    .settings(commonSettings)
    .settings(
      name := moduleName("shared", virtualAxes.value),
      libraryDependencies ++= {
        val version = virtualAxes.value.collectFirst { case c: PactJvmAxis => c.version }.get
        Dependencies.shared(version)
      }
    )

lazy val circe = (projectMatrix in file("circe"))
  .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java11), identity(_))
  .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java8), identity(_))
  .settings(
    name := moduleName("pact4s-circe", virtualAxes.value),
    libraryDependencies ++= Dependencies.circe,
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(shared)

lazy val munit =
  (projectMatrix in file("munit-cats-effect-pact"))
    .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java11), identity(_))
    .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java8), identity(_))
    .settings(
      name := moduleName("pact4s-munit-cats-effect", virtualAxes.value),
      libraryDependencies ++= Dependencies.munit,
      testFrameworks += new TestFramework("munit.Framework")
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val scalaTest =
  (projectMatrix in file("scalatest-pact"))
    .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java11), identity(_))
    .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java8), identity(_))
    .settings(
      name := moduleName("pact4s-scalatest", virtualAxes.value),
      libraryDependencies ++= Dependencies.scalatest
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val weaver = (projectMatrix in file("weaver-pact"))
  .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java11), identity(_))
  .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java8), identity(_))
  .settings(
    name := moduleName("pact4s-weaver", virtualAxes.value),
    libraryDependencies ++= Dependencies.weaver,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
  .dependsOn(shared % "compile->compile;test->test")
  .dependsOn(circe % "test->test")

lazy val pact4s = (projectMatrix in file("."))
  .settings(commonSettings)
  .enablePlugins(AutomateHeaderPlugin)
  .aggregate(
    munit,
    scalaTest,
    weaver,
    shared,
    circe
  )
