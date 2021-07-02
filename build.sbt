import commandmatrix.Dimension
import sbt.Keys.{resolvers, testFrameworks}

val scala212       = "2.12.14"
val scala213       = "2.13.6"
val scala2Versions = Seq(scala212, scala213)
val scala3         = "3.0.0-RC2"

sonatypeCredentialHost := Sonatype.sonatype01

val javaVersionDimension =
  Dimension.create("PACT_JVM") {
    case PactJvmAxis.java8  => "java8"
    case PactJvmAxis.java11 => "java11"
  }

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
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := scala213,
    commands ++= CrossCommand.single(
      "test",
      matrices = Seq(circe, munit, scalaTest, weaver),
      dimensions = Seq(
        javaVersionDimension,
        Dimension.scala("2.13"),
        Dimension.platform()
      )
    )
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

val moduleBase =
  Def.setting((Compile / scalaSource).value.getParentFile().getParentFile().getParentFile())

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
      },
      Compile / unmanagedSourceDirectories ++= {
        val version = virtualAxes.value.collectFirst { case c: PactJvmAxis => c.version }.get
        version match {
          case Dependencies.pactJvmJava11 =>
            Seq(
              moduleBase.value / s"src" / "main" / "java11+"
            )
          case Dependencies.pactJvmJava8 =>
            Seq(
              moduleBase.value / s"src" / "main" / "java8"
            )
          case _ => Nil
        }
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

addCommandAlias(
  "commitCheck",
  List("scalafmtAll", "test-java8-2_12-jvm", "test-java11-2_12-jvm", "test-java8-2_13-jvm", "test-java11-2_13-jvm")
    .mkString(";", ";", "")
)

addCommandAlias( //same as above but only tests 2.13
  "quickCommitCheck",
  List("scalafmtAll", "test-java8-2_13-jvm", "test-java11-2_13-jvm")
    .mkString(";", ";", "")
)
