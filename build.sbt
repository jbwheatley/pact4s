import sbt.Keys.*

val scala212         = "2.12.20"
val scala213         = "2.13.16"
val scala2Versions   = Seq(scala212, scala213)
val scala3           = "3.3.6"
val allScalaVersions = Seq(scala212, scala213, scala3)

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
    startYear          := Some(2021),
    licenses           := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion       := scala213,
    crossScalaVersions := allScalaVersions
  )
)

publish / skip := true // don't publish the root project

val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  ),
  Test / parallelExecution := false
)

val moduleBase =
  Def.setting((Compile / scalaSource).value.getParentFile.getParentFile.getParentFile)

lazy val models = (project in file("models"))
  .settings(commonSettings)
  .settings(name := "pact4s-models", libraryDependencies ++= Dependencies.models)

lazy val shared =
  (project in file("shared"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-core",
      libraryDependencies ++= Dependencies.shared,
      scalacOptions += "-Wconf:cat=deprecation:i"
    )
    .dependsOn(models)
    .dependsOn(circe % "test->test")

lazy val circe =
  (project in file("circe"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-circe",
      libraryDependencies ++= Dependencies.circe,
      Test / parallelExecution := true
    )
    .dependsOn(models)

lazy val playJson =
  (project in file("play-json"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-play-json",
      libraryDependencies ++= Dependencies.playJson,
      Test / parallelExecution := true
    )
    .dependsOn(models)

lazy val sprayJson =
  (project in file("spray-json"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-spray-json",
      libraryDependencies ++= Dependencies.sprayJson,
      Test / parallelExecution := true
    )
    .dependsOn(models)

lazy val munit =
  (project in file("munit-cats-effect-pact"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-munit-cats-effect",
      libraryDependencies ++= Dependencies.munit
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val scalaTest =
  (project in file("scalatest-pact"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-scalatest",
      libraryDependencies ++= Dependencies.scalatest
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val weaver =
  (project in file("weaver-pact"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-weaver",
      libraryDependencies ++= Dependencies.weaver,
      testFrameworks += new TestFramework("weaver.framework.CatsEffect")
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val zioTest =
  (project in file("ziotest-pact"))
    .settings(commonSettings)
    .settings(
      name := "pact4s-zio-test",
      libraryDependencies ++= Dependencies.zioTest,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
    )
    .dependsOn(shared % "compile->compile;test->test")
    .dependsOn(circe % "test->test")

lazy val exampleConsumer =
  (project in file("example/consumer"))
    .settings(commonSettings)
    .settings(
      name := "example-consumer",
      libraryDependencies ++= Dependencies.example,
      testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      publish / skip := true
    )
    .dependsOn(circe % Test, munit % Test, scalaTest % Test, weaver % "test->test", zioTest % Test)

lazy val exampleProvider =
  (project in file("example/provider"))
    .settings(commonSettings)
    .settings(
      name := "example-provider",
      libraryDependencies ++= Dependencies.example,
      testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      Test / parallelExecution := false,
      publish / skip           := true
    )
    .dependsOn(munit % Test, scalaTest % Test, weaver % "test->test", zioTest % Test)

lazy val pact4s = (project in file("."))
  .settings(commonSettings)
  .enablePlugins(AutomateHeaderPlugin)
  .aggregate(
    models,
    munit,
    scalaTest,
    weaver,
    zioTest,
    shared,
    circe,
    playJson,
    sprayJson,
    exampleConsumer,
    exampleProvider
  )

lazy val deletePactFiles = taskKey[Unit]("deletes pact files created during tests.")

deletePactFiles := {
  import java.io.File
  import scala.reflect.io.Directory
  List(scalaTest.base.base, munit.base.base, weaver.base.base, zioTest.base.base).foreach { project =>
    new Directory(new File(s"./$project/target/pacts")).deleteRecursively()
    ()
  }
}

addCommandAlias(
  "testCore",
  List(
    "clean",
    "scalafmtCheck",
    "headerCheck",
    "+compile:doc",
    "+test:compile",
    "deletePactFiles",
    "project models",
    "+test",
    "project shared",
    "+test",
    "project circe",
    "+test",
    "project playJson",
    "+test",
    "project sprayJson",
    "+test"
  )
    .mkString(";", ";", "")
)

addCommandAlias(
  "testScalaTest",
  List(
    "clean",
    "deletePactFiles",
    "project scalaTest",
    "+test"
  )
    .mkString(";", ";", "")
)

addCommandAlias(
  "testMunit",
  List(
    "clean",
    "deletePactFiles",
    "project munit",
    "+test"
  )
    .mkString(";", ";", "")
)

addCommandAlias(
  "testZioTest",
  List(
    "clean",
    "deletePactFiles",
    "project scalaTest",
    "+test"
  )
    .mkString(";", ";", "")
)

addCommandAlias(
  "testWeaver",
  List(
    "clean",
    "deletePactFiles",
    "project weaver",
    "+test"
  )
    .mkString(";", ";", "")
)

addCommandAlias(
  "testExamples",
  List(
    "clean",
    "deletePactFiles",
    "project exampleConsumer",
    "+test",
    "project exampleProvider",
    "+test"
  )
    .mkString(";", ";", "")
)

addCommandAlias(
  "commitCheck",
  (List("clean", "deletePactFiles", "scalafmtCheck", "headerCheck", "+compile:doc", "+test:compile") ++
    List(
      "models",
      "munit",
      "scalaTest",
      "weaver",
      "zioTest",
      "shared",
      "circe",
      "playJson",
      "sprayJson",
      "exampleConsumer",
      "exampleProvider"
    ).flatMap(p => List(s"project $p", "+test")))
    .mkString(";", ";", "")
)
