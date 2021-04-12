import sbt._

object Dependencies {
  val pactJvm = "4.2.0"

  val shared: Seq[ModuleID] = Seq(
    "au.com.dius.pact" % "consumer" % pactJvm,
    "org.scalaj" %% "scalaj-http" % "2.4.2" % Test,
    "org.log4s" %% "log4s" % "1.8.2",
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
