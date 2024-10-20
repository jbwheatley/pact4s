import sbt._

object Dependencies {

  val mockito = "5.14.2"

  val pactJvm = "4.6.14"

  val http4s = "0.23.28"

  val log4s = "1.10.0"

  val collectionCompat = "2.12.0"

  val sourcecode = "0.4.2"

  val _circe = "0.14.10"

  val _playJson = "3.0.4"

  val _sprayJson = "1.3.6"

  val _weaver = "0.8.4"

  val _scalatest = "3.2.19"

  val _munit = "1.0.2"

  val _zio = "2.1.11"

  val munitCatsEffect = "2.0.0"

  val upickle = "4.0.2"

  val zioCats = "23.1.0.3"

  val models: Seq[ModuleID] = Seq(
    "au.com.dius.pact"        % "consumer"                % pactJvm,
    "au.com.dius.pact"        % "provider"                % pactJvm,
    "org.scala-lang.modules" %% "scala-collection-compat" % collectionCompat,
    "org.scalameta"          %% "munit"                   % _munit % Test
  )

  val shared: Seq[ModuleID] =
    Seq(
      "org.log4s"   %% "log4s"               % log4s,
      "com.lihaoyi" %% "sourcecode"          % sourcecode,
      "com.lihaoyi" %% "upickle"             % upickle,
      "org.http4s"  %% "http4s-ember-client" % http4s  % Test,
      "org.http4s"  %% "http4s-dsl"          % http4s  % Test,
      "org.http4s"  %% "http4s-ember-server" % http4s  % Test,
      "org.http4s"  %% "http4s-circe"        % http4s  % Test,
      "io.circe"    %% "circe-core"          % _circe  % Test,
      "org.mockito"  % "mockito-core"        % mockito % Test
    )

  val munit: Seq[ModuleID] = Seq(
    "org.typelevel" %% "munit-cats-effect" % munitCatsEffect % Provided
  )

  val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % _scalatest % Provided
  )

  val weaver: Seq[ModuleID] = Seq(
    "com.disneystreaming" %% "weaver-core" % _weaver % Provided,
    "com.disneystreaming" %% "weaver-cats" % _weaver % Test
  )

  val zioTest: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio"          % _zio % Provided,
    "dev.zio" %% "zio-test"     % _zio % Provided,
    "dev.zio" %% "zio-test-sbt" % _zio % Provided
  )

  val circe: Seq[ModuleID] = Seq(
    "io.circe"      %% "circe-core"   % _circe,
    "io.circe"      %% "circe-parser" % _circe,
    "org.scalameta" %% "munit"        % _munit % Test
  )

  val playJson: Seq[ModuleID] = Seq(
    "org.playframework" %% "play-json" % _playJson,
    "org.scalameta"     %% "munit"     % _munit % Test
  )

  val sprayJson: Seq[ModuleID] = Seq(
    ("io.spray"     %% "spray-json" % _sprayJson).cross(CrossVersion.for3Use2_13),
    "org.scalameta" %% "munit"      % _munit % Test
  )

  val example: Seq[ModuleID] = Seq(
    "org.http4s"    %% "http4s-ember-client" % http4s,
    "org.http4s"    %% "http4s-dsl"          % http4s,
    "org.http4s"    %% "http4s-ember-server" % http4s,
    "org.http4s"    %% "http4s-circe"        % http4s,
    "io.circe"      %% "circe-core"          % _circe,
    "dev.zio"       %% "zio-interop-cats"    % zioCats         % Test,
    "dev.zio"       %% "zio-test"            % _zio            % Test,
    "dev.zio"       %% "zio-managed"         % _zio            % Test,
    "org.typelevel" %% "munit-cats-effect"   % munitCatsEffect % Test,
    "org.scalatest" %% "scalatest"           % _scalatest      % Test
  )

}
