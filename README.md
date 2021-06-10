# pact4s

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jbwheatley/pact4s-weaver_2.13.svg)](http://search.maven.org/#search%7Cga%7C1%7Cpact4s)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

Lightweight wrapper of pact-jvm for commonly used scala testing frameworks.

Currently supports writing consumer pact tests and verifying pacts from either a file or a pact-broker in `munit-cats-effect`, `weaver`, and `scala-test`. Add one of the following dependencies to your project to use: 
```
"io.github.jbwheatley" %% "pact4s-munit-cats-effect" % xxx
"io.github.jbwheatley" %% "pact4s-weaver"            % xxx
"io.github.jbwheatley" %% "pact4s-scalatest"         % xxx
```

## Writing Consumer Request/Response Pacts

The modules `pact4s-munit-cats-effect`, `pact4s-weaver` and `pact4s-scalatest` mixins share the following interfaces for defining pacts:

```scala

//Can override the server address for the mock provider, as well as the pact spec version. 
//Defaults to http://localhost:0 and spec version 3. 
override val mockProviderConfig: MockProviderConfig = new MockProviderConfig("localhost", 1234, PactSpecVersion.V3, "http")

//Can override where the pacts files get written to before before being published. Defaults to "./target/pacts"
override val pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext(
  "path/to/pact/directory"
)

val pact: RequestResponsePact =
  ConsumerPactBuilder
    .consumer("Consumer")
    .hasPactWith("Provider")
    .uponReceiving("a request to say Hello")
    .path("/hello")
    .method("POST")
    .body("""{"json": "body"}""", "application/json")
    .headers("other-header" -> "howdy")
    .willRespondWith()
    .status(200)
    .body("""{"response": "body"}""")
    .uponReceiving("a request to say Goodbye")
    .path("/goodbye")
    .method("GET")
    .willRespondWith()
    .status(204)
    .toPact()
      
```

Pacts are constructed using the pact-jvm dsl, but with additional helpers for easier interop with scala. For example, anywhere a java `Map` is expected, a scala `Map`, or scala tuples can be provided instead. 

If you want to construct simple pacts with bodies that do not use the pact-jvm matching dsl, (`PactDslJsonBody`), a scala data type `A` can be passed to `.body` directly, provided there is an implicit instance of `pact4s.PactBodyEncoder[A]` provided. By adding the additional dependency `io.github.jbwheatley %% pact4s-circe % xxx`, instances of `pact4s.PactBodyEncoder` are provided for any type that has a `circe.Encoder` instance. This allows the following: 

```scala
import pact4s.circe.implicits._

final case class Foo(a: String)

implicit val encoder: Encoder[Foo] = ???

val pact: RequestResponsePact =
  ConsumerPactBuilder
    .consumer("Consumer")
    .hasPactWith("Provider")
    .uponReceiving("a request to say Hello")
    .path("/hello")
    .method("POST")
    .body(Foo("abcde"), "application/json")
    ...
```

Each of the APIs for writing consumer pact tests provided by each of `pact4s-munit-cats-effect`, `pact4s-weaver` and `pact4s-scalatest` are slightly different to account for the differences between the APIs of the testing frameworks. We recommend looking at the following test suites for examples of how each of these APIs looks: 

- [munit-cats-effect](https://github.com/jbwheatley/pact4s/blob/549bc3e76d3e377862438f6f748075499cc39f7d/munit-cats-effect-pact/src/test/scala/pact4s/munit/RequestResponsePactForgerMUnitSuite.scala) 
- [weaver](https://github.com/jbwheatley/pact4s/blob/549bc3e76d3e377862438f6f748075499cc39f7d/weaver-pact/src/test/scala/pact4s/weaver/RequestResponsePactForgerWeaverSuite.scala)
- [scalatest](https://github.com/jbwheatley/pact4s/blob/549bc3e76d3e377862438f6f748075499cc39f7d/scalatest-pact/src/test/scala/pact4s/scalatest/RequestResponsePactForgerScalaTestSuite.scala)

## Publishing Consumer Request/Response Pacts

This library does not (and won't ever) provide native support for publishing consumer pacts to the pact broker. For this, we recommend using the `Pact Broker CLI` provided by the pact foundation as part of your CI pipeline: https://github.com/pact-foundation/pact_broker-client

If you have previously been relying on the [`scala-pact`](https://github.com/ITV/scala-pact) sbt plugin to publish pacts to a pact broker, compatability with pacts produced by pact-jvm was added in version 3.3.1. By adding the sbt setting `areScalaPactContracts := false`, the scala-pact plugin will be able to publish pacts produced by this library, and any other pact-jvm based consumer pact testing library.

## Verifying Request/Response Pacts

Verification can either be done as part of your CI pipeline, again by using the `Pact Broker CLI`, or by writing a verification test within your project. The test modules in `pact4s` share the following interface for how pacts are retrieved from either a pact broker, or a file: 

```scala
override val provider: ProviderInfoBuilder = 
  ProviderInfoBuilder(
    name = "Provider",
    protocol = "http",
    host = "localhost",
    port = 1234,
    path = "/",
    pactSource = ???
  )
```

`PactSource` is an ADT that providers various different configurations for fetching pacts. More can be learned here about the how the pact-broker works here: https://docs.pact.io/pact_broker

After defining the `provider`, the verification step can be run against your mock provider simply by adding a test that has the following body: 
```scala
test("verify pacts") {
  verifyPacts()
}
```

Please note, due to the version of pact-jvm that is underpinning `pact4s`, the verification step uses the `Pacts For Verification` API in the pact broker. See this issue here for more information: https://github.com/pact-foundation/pact_broker/issues/307. This may not be available in earlier versions of the pact-broker, so make sure you are using the latest release of the broker. 

Pacts produced by pact-jvm (and by extension pact4s) by default conform to V3 of the pact specification, which *CANNOT* be verified by `scala-pact`.

## Prerequisites

Due to the java version used by the underlying pact-jvm library, you won't be able to use this wrapper unless your project is being built on java 11+. If you build on java 8 for example, you might see something like this: 

```
java.lang.UnsupportedClassVersionError: au/com/dius/pact/core/model/BasePact has been compiled by a more recent version of the Java Runtime (class file version
55.0), this version of the Java Runtime only recognizes class file versions up to 52.0
```
