import sbt._

object Dependencies {
  val mockitoScala     = "1.16.49"
  val pactJvm          = "4.3.2"
  val http4s           = "1.0.0-M30"
  val log4s            = "1.10.0"
  val logback          = "1.2.9"
  val collectionCompat = "2.6.0"
  val sourcecode       = "0.2.7"
  val _circe           = "0.14.1"
  val _playJson        = "2.9.2"
  val _weaver          = "0.7.9"
  val _scalatest       = "3.2.10"
  val _munit           = "0.7.29"
  val munitCatsEffect  = "1.0.7"

  val shared: Seq[ModuleID] =
    Seq(
      "au.com.dius.pact"        % "consumer"                % pactJvm,
      "au.com.dius.pact"        % "provider"                % pactJvm,
      "org.log4s"              %% "log4s"                   % log4s,
      "ch.qos.logback"          % "logback-classic"         % logback         % Runtime,
      "org.scala-lang.modules" %% "scala-collection-compat" % collectionCompat,
      "com.lihaoyi"            %% "sourcecode"              % sourcecode,
      "org.http4s"             %% "http4s-ember-client"     % http4s          % Test,
      "org.http4s"             %% "http4s-dsl"              % http4s          % Test,
      "org.http4s"             %% "http4s-ember-server"     % http4s          % Test,
      "org.http4s"             %% "http4s-circe"            % http4s          % Test,
      "io.circe"               %% "circe-core"              % _circe          % Test,
      "org.mockito"            %% "mockito-scala"           % mockitoScala    % Test,
      "org.typelevel"          %% "munit-cats-effect-3"     % munitCatsEffect % Test
    )

  val munit: Seq[ModuleID] = Seq(
    "org.typelevel" %% "munit-cats-effect-3" % munitCatsEffect % Provided
  )

  val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % _scalatest % Provided
  )

  val weaver: Seq[ModuleID] = Seq(
    "com.disneystreaming" %% "weaver-core" % _weaver % Provided,
    "com.disneystreaming" %% "weaver-cats" % _weaver % Test
  )

  val circe: Seq[ModuleID] = Seq(
    "io.circe"      %% "circe-core"   % _circe,
    "io.circe"      %% "circe-parser" % _circe,
    "org.scalameta" %% "munit"        % _munit % Test
  )

  val playJson: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-json" % _playJson,
    "org.scalameta"     %% "munit"     % _munit % Test
  )

}
