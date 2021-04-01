import sbt._

object Dependencies {
  val pactJvm = "4.2.0"

  val all: Seq[ModuleID] = Seq(
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.1",
    "au.com.dius.pact" % "consumer" % pactJvm,
    "org.scalaj" %% "scalaj-http" % "2.4.2" % Test,
    "org.log4s" %% "log4s" % "1.8.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime
  )
}
