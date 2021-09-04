import sbt._

object Dependencies {
  val pactJvmJava11 = "4.2.11"
  val pactJvmJava8  = "4.1.26"
  val http4s        = "1.0.0-M25"
  val _circe        = "0.14.1"

  def shared(pactJvmVersion: String): Seq[ModuleID] =
    Seq(
      "au.com.dius.pact"        % "consumer"                % pactJvmVersion,
      "au.com.dius.pact"        % "provider"                % pactJvmVersion,
      "org.http4s"             %% "http4s-ember-client"     % http4s  % Test,
      "org.http4s"             %% "http4s-dsl"              % http4s  % Test,
      "org.http4s"             %% "http4s-ember-server"     % http4s  % Test,
      "org.http4s"             %% "http4s-circe"            % http4s  % Test,
      "io.circe"               %% "circe-core"              % _circe  % Test,
      "org.log4s"              %% "log4s"                   % "1.10.0",
      "ch.qos.logback"          % "logback-classic"         % "1.2.5" % Runtime,
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.5.0",
      "com.lihaoyi"            %% "sourcecode"              % "0.2.7"
    )

  val munit: Seq[ModuleID] = Seq(
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.5" % Provided
  )

  val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.2.9" % Provided
  )

  val weaver: Seq[ModuleID] = Seq(
    "com.disneystreaming" %% "weaver-core" % "0.7.4" % Provided,
    "com.disneystreaming" %% "weaver-cats" % "0.7.4" % Test
  )

  val circe: Seq[ModuleID] = Seq(
    "io.circe"      %% "circe-core"          % _circe,
    "io.circe"      %% "circe-parser"        % _circe,
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.5" % Test
  )
}
