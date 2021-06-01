import sbt._

object Dependencies {
  val pactJvm = "4.2.6"
  val http4s  = "1.0.0-M23"
  val _circe  = "0.14.1"

  val shared: Seq[ModuleID] = Seq(
    "au.com.dius.pact"        % "consumer"                % pactJvm,
    "au.com.dius.pact"        % "provider"                % pactJvm,
    "org.http4s"             %% "http4s-ember-client"     % http4s  % Test,
    "org.http4s"             %% "http4s-dsl"              % http4s  % Test,
    "org.http4s"             %% "http4s-ember-server"     % http4s  % Test,
    "org.http4s"             %% "http4s-circe"            % http4s  % Test,
    "io.circe"               %% "circe-core"              % _circe  % Test,
    "org.log4s"              %% "log4s"                   % "1.10.0",
    "ch.qos.logback"          % "logback-classic"         % "1.2.3" % Runtime,
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4"
  )

  val munit: Seq[ModuleID] = Seq(
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.3" % Provided
  ) ++ shared

  val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.2.9" % Provided
  ) ++ shared

  val weaver: Seq[ModuleID] = Seq(
    "com.disneystreaming" %% "weaver-core" % "0.7.3" % Provided,
    "com.disneystreaming" %% "weaver-cats" % "0.7.3" % Test
  ) ++ shared

  val circe: Seq[ModuleID] = Seq(
    "io.circe"      %% "circe-core"          % _circe,
    "io.circe"      %% "circe-parser"        % _circe,
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.3" % Test
  )
}
