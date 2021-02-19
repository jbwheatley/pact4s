name := "pact-weaver"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Dependencies.all

resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)

testFrameworks += new TestFramework("weaver.framework.CatsEffect")