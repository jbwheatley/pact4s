//package pact4s.sbt
//
//import sbt.AutoPlugin
//import sbt.Keys._
//import sbt.{Def, _}
//import complete.DefaultParsers._
//
//object Pact4sPlugin extends AutoPlugin {
//
//  object autoImport {
//    val pactPublishSettings: SettingKey[PactPublishSettings] = SettingKey[PactPublishSettings]("")
//
//    val pactPublish: InputKey[Unit] = inputKey[Unit]("publishing pact files to a pact-broker host")
//  }
//
//  import autoImport._
//
//  override lazy val projectSettings: Seq[Def.Setting[_]] =
//    Seq(
//      pactPublishSettings := PactPublishSettings(),
//      pactPublish := pactPublishTask.evaluated
//    )
//
//  def pactPublishTask: Def.Initialize[InputTask[Unit]] =
//    Def.inputTask {
//      ()
//    }
//
//}
