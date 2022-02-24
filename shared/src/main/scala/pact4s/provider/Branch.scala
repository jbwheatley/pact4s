package pact4s
package provider

final case class Branch(branch: String)

object Branch {
  val MAIN: Branch = Branch("main")
  val MASTER: Branch = Branch("master")
}