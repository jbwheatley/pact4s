import sbt._

object Dependencies {
  val pactJvm = "4.2.0"
  val http4s = "1.0.0-M21"

  val shared: Seq[ModuleID] = Seq(
    "au.com.dius.pact" % "consumer" % pactJvm,
    "org.http4s" %% "http4s-ember-client" % http4s % Test,
    "org.http4s" %% "http4s-dsl" % http4s % Test,
    "org.log4s" %% "log4s" % "1.10.0-M6",
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime
  )

  val munit: Seq[ModuleID] = Seq(
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.1"
  ) ++ shared

  val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.2.7"
  ) ++ shared

  val weaver: Seq[ModuleID] = Seq(
    "com.disneystreaming" %% "weaver-core" % "0.7.0",
    "com.disneystreaming" %% "weaver-cats" % "0.7.0" % Test
  ) ++ shared
}
