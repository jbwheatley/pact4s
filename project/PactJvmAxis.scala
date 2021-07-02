import sbt.VirtualAxis

case class PactJvmAxis(series: String, version: String) extends VirtualAxis.WeakAxis {
  val directorySuffix = series
  val idSuffix        = series
}

object PactJvmAxis {
  lazy val java11 = PactJvmAxis("", Dependencies.pactJvmJava11)
  lazy val java8  = PactJvmAxis("-java8", Dependencies.pactJvmJava8)
}
