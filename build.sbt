import commandmatrix.Dimension
import sbt.internal.ProjectMatrix
import sbt.Keys.{resolvers, testFrameworks}

import scala.util.Try

val scala212       = "2.12.15"
val scala213       = "2.13.7"
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
    homepage     := Some(url("https://github.com/jbwheatley/pact4s")),
    developers := List(
      Developer(
        "jbwheatley",
        "jbwheatley",
        "jbwheatley@proton.me",
        url("https://github.com/jbwheatley")
      )
    ),
    licenses     := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := scala213,
    commands ++= CrossCommand.single(
      "test",
      matrices = Seq(shared, circe, munit, scalaTest, weaver),
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
  // When using IntelliJ, don't import projects for Scala 2.12.x or Java 8. This prevents classpath conflicts.
  // See: https://github.com/sbt/sbt-projectmatrix/issues/25
  ideSkipProject.withRank(KeyRanks.Invisible) := scalaVersion.value.startsWith("2.12.") || name.value.contains("java8"),
  resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  )
)

def moduleName(base: String, axis: Seq[VirtualAxis]): String = {
  val series = axis.collectFirst { case c: PactJvmAxis => c.series }.get
  s"$base$series"
}

val withStandardSettings: ProjectMatrix => ProjectMatrix =
  _.customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java11), identity(_))
    .customRow(scalaVersions = scala2Versions, axisValues = Seq(VirtualAxis.jvm, PactJvmAxis.java8), identity(_))
    .settings(commonSettings)

val moduleBase =
  Def.setting((Compile / scalaSource).value.getParentFile.getParentFile.getParentFile)

lazy val shared =
  withStandardSettings(projectMatrix in file("shared"))
    .settings(
      name := moduleName("pact4s-core", virtualAxes.value),
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

lazy val circe =
  withStandardSettings(projectMatrix in file("circe"))
    .settings(
      name := moduleName("pact4s-circe", virtualAxes.value),
      libraryDependencies ++= Dependencies.circe,
      Test / unmanagedSourceDirectories ++= {
        val version = virtualAxes.value.collectFirst { case c: PactJvmAxis => c.version }.get
        version match {
          case Dependencies.pactJvmJava11 =>
            Seq(
              moduleBase.value / s"src" / "test" / "java11+"
            )
          case Dependencies.pactJvmJava8 =>
            Seq(
              moduleBase.value / s"src" / "test" / "java8"
            )
          case _ => Nil
        }
      }
    )
    .dependsOn(shared)

lazy val munit =
  withStandardSettings(projectMatrix in file("munit-cats-effect-pact"))
    .settings(
      name := moduleName("pact4s-munit-cats-effect", virtualAxes.value),
      libraryDependencies ++= Dependencies.munit
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val scalaTest =
  withStandardSettings(projectMatrix in file("scalatest-pact"))
    .settings(
      name := moduleName("pact4s-scalatest", virtualAxes.value),
      libraryDependencies ++= Dependencies.scalatest
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val weaver =
  withStandardSettings(projectMatrix in file("weaver-pact"))
    .settings(
      name := moduleName("pact4s-weaver", virtualAxes.value),
      libraryDependencies ++= Dependencies.weaver,
      testFrameworks ++= {
        if (Try(System.getenv("TEST_WEAVER").toBoolean).getOrElse(true))
          Seq(new TestFramework("weaver.framework.CatsEffect"))
        else Nil
      }
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
  List(
    "scalafmtCheck",
    "compile:doc",
    "test-java8-2_12-jvm",
    "test-java11-2_12-jvm",
    "test-java8-2_13-jvm",
    "test-java11-2_13-jvm"
  )
    .mkString(";", ";", "")
)

addCommandAlias( //same as above but only tests 2.13
  "quickCommitCheck",
  List("scalafmtCheck", "compile:doc", "test-java8-2_13-jvm", "test-java11-2_13-jvm")
    .mkString(";", ";", "")
)
