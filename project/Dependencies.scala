import sbt._

object Dependencies {
  val weaver = "0.6.0-M6"
  val pactJvm = "4.2.0"
  val http4s = "1.0.0-M16"


  val all: Seq[ModuleID] = Seq(
    "com.disneystreaming" %% "weaver-cats" % weaver,
    "au.com.dius.pact" % "consumer" % pactJvm,
    "org.scalaj" %% "scalaj-http" % "2.4.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime
  )
}
