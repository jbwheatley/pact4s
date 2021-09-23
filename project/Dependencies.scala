import sbt._

object Dependencies {
  val mockitoScala     = "1.16.42"
  val pactJvmJava11    = "4.2.11"
  val pactJvmJava8     = "4.1.26"
  val http4s           = "1.0.0-M27"
  val pactJvmJava8     = "4.1.27"
  val http4s           = "1.0.0-M27"
  val log4s            = "1.10.0"
  val logback          = "1.2.6"
  val collectionCompat = "2.5.0"
  val sourcecode       = "0.2.7"
  val _circe           = "0.14.1"
  val _weaver          = "0.7.6"
  val _scalatest       = "3.2.10"
  val _munit           = "1.0.5"

  def shared(pactJvmVersion: String): Seq[ModuleID] =
    Seq(
      "au.com.dius.pact"        % "consumer"                % pactJvmVersion,
      "au.com.dius.pact"        % "provider"                % pactJvmVersion,
      "org.http4s"             %% "http4s-ember-client"     % http4s       % Test,
      "org.http4s"             %% "http4s-dsl"              % http4s       % Test,
      "org.http4s"             %% "http4s-ember-server"     % http4s       % Test,
      "org.http4s"             %% "http4s-circe"            % http4s       % Test,
      "io.circe"               %% "circe-core"              % _circe       % Test,
      "org.mockito"            %% "mockito-scala"           % mockitoScala % Test,
      "org.log4s"              %% "log4s"                   % log4s,
      "ch.qos.logback"          % "logback-classic"         % logback      % Runtime,
      "org.scala-lang.modules" %% "scala-collection-compat" % collectionCompat,
      "com.lihaoyi"            %% "sourcecode"              % sourcecode
    )

  val munit: Seq[ModuleID] = Seq(
    "org.typelevel" %% "munit-cats-effect-3" % _munit % Provided
  )

  val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % _scalatest % Provided
  )

  val weaver: Seq[ModuleID] = Seq(
    "com.disneystreaming" %% "weaver-core" % _weaver % Provided,
    "com.disneystreaming" %% "weaver-cats" % _weaver % Test
  )

  val circe: Seq[ModuleID] = Seq(
    "io.circe"      %% "circe-core"          % _circe,
    "io.circe"      %% "circe-parser"        % _circe,
    "org.typelevel" %% "munit-cats-effect-3" % _munit % Test
  )
}
