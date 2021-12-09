# pact4s

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jbwheatley/pact4s-weaver_2.13.svg)](http://search.maven.org/#search%7Cga%7C1%7Cpact4s)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

Lightweight wrapper of [pact-jvm](https://github.com/pact-foundation/pact-jvm) for commonly used scala testing frameworks. To find out more about consumer-driven contract testing, visit the Pact Foundation website [here](https://docs.pact.io/)! Supported scala versions are 2.12 and 2.13.<sup>1</sup>

`pact4s` is still in the early stages of development! Please consider helping us out by contributing or raising issues :)

## Getting Started

This library provides support for `munit-cats-effect`, `weaver`, and `scalatest`, to write and verify both request/response and message pacts. The underlying library, pact-jvm, is currently supported on two branches, depending on the jdk version: 

| Branch | Pact Spec | JDK |
| ------ | ------------- | --- | 
| [4.2.x](https://github.com/DiUS/pact-jvm/blob/v4.2.x/README.md) | V4* | 11+ |
| [4.1.x](https://github.com/DiUS/pact-jvm/blob/v4.1.x/README.md) | V3 | 8-12 |

All the modules in `pact4s` are built against both of these branches to accommodate all jdk versions. To use the java11+ modules, simply add one of the following dependencies to your project: 
```
"io.github.jbwheatley" %% "pact4s-munit-cats-effect" % xxx
"io.github.jbwheatley" %% "pact4s-weaver"            % xxx
"io.github.jbwheatley" %% "pact4s-scalatest"         % xxx
```

We recommend using these modules if possible, as v4.2.x+ of pact-jvm will see longer continued support. But, if you are unable to use java11+ for your build, add one of the following to your project instead:
```
"io.github.jbwheatley" %% "pact4s-munit-cats-effect-java8" % xxx
"io.github.jbwheatley" %% "pact4s-weaver-java8"            % xxx
"io.github.jbwheatley" %% "pact4s-scalatest-java8"         % xxx
```

We also offer some additional helpers for using JSON encoders directly in your pact definitions. Currently, support is offered for `circe` in the modules `pact4s-circe`/`pact4s-circe-java8`. If you would like to see support for your favourite scala JSON library, consider submitting a PR!

**N.B.** If you try and use the non-java8 module versions, and your project is built on java8, you will see an error like this:

```
java.lang.UnsupportedClassVersionError: au/com/dius/pact/core/model/BasePact has been compiled by a more recent version of the Java Runtime (class file version
55.0), this version of the Java Runtime only recognizes class file versions up to 52.0
```

---

<sup>1</sup> support for scala 3 is currently blocked by https://github.com/lampepfl/dotty/issues/12086, as pact-jvm is written in kotlin

## Writing Pacts

The modules `pact4s-munit-cats-effect`, `pact4s-weaver` and `pact4s-scalatest` mixins all share common interfaces for defining pacts. The APIs for each of these modules is slightly different to account for the differences between the APIs of the testing frameworks. We recommend looking at the tests in this project for examples of each.

### Pact Builder DSL

Pacts are constructed using the pact-jvm DSL, but with additional helpers for easier interoperability with scala. For example, anywhere a java `Map` is expected, a scala `Map`, or scala tuples can be provided instead.

#### Using JSON bodies

If you want to construct simple pacts with bodies that do not use the pact-jvm matching dsl, (`PactDslJsonBody`), a scala data type `A` can be passed to `.body` directly, provided there is an implicit instance of `pact4s.PactBodyEncoder[A]` provided.

Instances of `pact4s.PactBodyEncoder` are provided for:
- any type that has a `circe.Encoder` by adding the additional dependency: ```io.github.jbwheatley %% pact4s-circe % xxx```
- any type that has a `play.api.libs.json.Writes` by adding the additional dependency: ```io.github.jbwheatley %% pact4s-play-json % xxx```

This allows the following when using the import `pact4s.circe.implicits._`:
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
    // ...
```

Or the following when using the import `pact4s.playjson.implicits._`:
```scala
import pact4s.playjson.implicits._

final case class Foo(a: String)

implicit val reads: Writes[Foo] = ???

val pact: RequestResponsePact =
  ConsumerPactBuilder
    .consumer("Consumer")
    .hasPactWith("Provider")
    .uponReceiving("a request to say Hello")
    .path("/hello")
    .method("POST")
    .body(Foo("abcde"), "application/json")
    // ...
```

### Request/Response Pacts

Request/response pacts use the `RequestResponsePactForger` trait. This trait requires that you provide a `RequestResponsePact`, which will be used to stand up a stub of the provider server. Each interaction in the pact should then run against the stub server using client the consumer application uses to interact with the real provider. This ensures that the client, and thus the application, is compatible with the pact being defined.

An example `RequestResponsePactForger` implementation is shown below.
```scala
val pact: RequestResponsePact =
  ConsumerPactBuilder
    .consumer("Consumer")
    .hasPactWith("Provider")
    .uponReceiving("a request to say Hello") // this is the description
    .path("/hello")
    .method("POST")
    .body("""{"json": "body"}""", "application/json")
    .headers("other-header" -> "howdy")
    .willRespondWith()
    .status(200)
    .body("""{"response": "body"}""")
    .toPact()

// The client your application uses to consume from the provider
val client: Client = new Client(mockServer.getUrl)

// Now loop through each interaction in the Pact and verify it.
// This is psuedo-code that will need to be adapted to the testing framework you are using.
interactions.foreach(verify)
def verify(interaction: RequestResponseInteraction): Result = interaction.getDescription match {
  case "a request to say Hello" =>
    val response = client(interaction.getRequest)
    assert(response == interaction.getResponse)
  case description =>
    throw NoSuchElementException(s"Missing verification for interaction: '$description'.")
}
```

Examples:
- [munit-cats-effect](https://github.com/jbwheatley/pact4s/blob/main/munit-cats-effect-pact/src/test/scala/pact4s/munit/RequestResponsePactForgerMUnitSuite.scala)
- [scalatest](https://github.com/jbwheatley/pact4s/blob/main/scalatest-pact/src/test/scala/pact4s/scalatest/RequestResponsePactForgerScalaTestSuite.scala)
- [weaver](https://github.com/jbwheatley/pact4s/blob/main/weaver-pact/src/test/scala/pact4s/weaver/RequestResponsePactForgerWeaverSuite.scala)

### Message Pacts

Message pacts use the `MessagePactForger` trait. This trait requires that you provide a `MessagePact`. While the general principles of message forging and verification are the same as with request/response pacts, the guidance here will be a bit more abstract as actual implementations will vary by application and messaging framework. That said, at a high level you will want to generate a message and then feed it to your message handling function, which should expect a concrete class type. You do not want to verify what the message handling function does, only that it can receive the message payload without exception.

An example `MessagePactForger` implementation is shown below:
```scala
val pact: MessagePact = Pact4sMessagePactBuilder()
  .consumer("MessageConsumer")
  .hasPactWith("MessageProvider")
  .expectsToReceive("A message to say hello")
  .withContent(Json.obj("hello" -> "harry".asJson))
  .withMetadata(Map("hi" -> "there"))
  .toMessagePact

// You will need access to the methods that actually handle your messages, for example on the application.
val application: Application = new Application()

// Now loop through each message and verify it.
// This is psuedo-code that will need to be adapted to the testing framework you are using.
messages.foreach(verify)
def verify(message: Message): Result = message.getDescription match {
  case "a request to say hello" =>
    // You will probably need to convert the Pact Message into some other format...
    val applicationMessage = new ApplicationMessage(message.contentAsBytes, message.metadata)
    assertNoException(application.handleMessage(applicationMessage))
  case description =>
    throw NoSuchElementException(s"Missing verification for message: '$description'.")
}
```

If your application framework supports it, another option would be to publish the Pact message and have your application consume it without error. Note that this would be testing more than just the Pact itself, though, and may or may not be beneficial to you.

### Mixed Pacts

Note that if your project has both request/response and message pacts, you will need to write them into separate pact files due to [pact-jvm not currently supporting mixed pacts](https://github.com/pact-foundation/pact-jvm/issues/610). Pact JSON files are written in the format `<consumer_name>_<provider_name>.json`. Realistically, this means that you will need to choose a different provider name for the message pacts, the request/response pacts, or both. For example, you could use `api.provider` as the provider name for the request/response pacts, or `message.provider` as the provider name for the message pacts.

## Publishing Pacts

This library does not (and won't ever) provide native support for publishing consumer pacts to the pact broker. For this, we recommend using the [Pact Broker CLI](https://github.com/pact-foundation/pact_broker-client) provided by the pact foundation as part of your CI pipeline.

If you have previously been relying on the [`scala-pact`](https://github.com/ITV/scala-pact) sbt plugin to publish pacts to a pact broker, compatability with pacts produced by pact-jvm was added in version 3.3.1. By adding the sbt setting `areScalaPactContracts := false`, the scala-pact plugin will be able to publish pacts produced by this library, and any other pact-jvm based consumer pact testing library.

## Verifying Pacts

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

`PactSource` is an ADT that provides various different configurations for fetching pacts, either from the local filesystem or from a [Pact Broker](https://docs.pact.io/pact_broker).

### Pact Broker

Please note, due to the version of pact-jvm that is underpinning `pact4s`, the verification step uses the `Pacts For Verification` API in the pact broker. See this issue here for more information: https://github.com/pact-foundation/pact_broker/issues/307. This may not be available in earlier versions of the pact-broker, so make sure you are using the latest release of the broker.

Pacts produced by pact-jvm (and by extension pact4s) by default conform to V3 of the pact specification, which *CANNOT* be verified by `scala-pact`.

### Request/Response Pact Verification

Verification of request/response pacts is extremely simple. You will want to extend the trait `PactVerifier` and set up a `ProviderInfoBuilder` (see above), which determines where the Pact files come from. Then you can verify them against your application by calling `verifyPacts`.

```scala
verifyPacts(
  // In this example, the results of verification are being uploaded to the Pact Broker
  publishVerificationResults = Some(
    PublishVerificationResults(
      // Normally this would be a version supplied by the build system, e.g. the Git commit hash, or a semantic version
      // like "1.0.0". See: https://docs.pact.io/getting_started/versioning_in_the_pact_broker
      providerVersion = "SNAPSHOT",
      // Normally this would be the git branch, e.g. "main" or "master"
      // See: https://docs.pact.io/pact_broker/tags/
      providerTags = Nil
    )
  )
)
```

The `verifyPacts` method will send requests generated from the pact to your application, and then verify the response it gets back, also against the pact.

#### Request Filtering

It is sometimes necessary to modify the request that pact-jvm generates before it reaches your application. One common use-case for this is the injection of [Authorization headers](https://docs.pact.io/provider/handling_auth/) into the requests.

```scala
val provider: ProviderInfoBuilder = 
  ProviderInfoBuilder()
    // This will add an Authorization header with a bearer token to every request
    .withRequestFilter(request => List(ProviderRequestFilter.SetHeaders("Authorization" -> "bearer <token>")))
```

See [ProviderInfoBuilder](./shared/src/main/scala/pact4s/ProviderInfoBuilder.scala) for more options.

### Message Pact Verification

Verification of message pacts is a little more abstract. You will want to extend the trait `MessagePactVerifier` and set up your `ProviderInfoBuilder` and `verifyPacts` methods just like you would for request/response pacts. You will then need to supply the messages for verification.

```scala
def messages: String => MessageAndMetadataBuilder = {
  case "A message to say hello" =>
    // This is psuedo-code. Normally these data would come from your application implementation.
    // For example, if you have a method which generates the message to publish, you could capture that value and
    // convert it to a MessageAndMetadataBuilder here.
    val metadata = Map("hi" -> "there")
    val body     = """{"hello":"harry"}"""
    MessageAndMetadataBuilder(body, metadata)
  case description =>
    throw new NoSuchElementException(s"Missing generator for message: '$description'")
}
```

### Provider state

Some pacts have requirements on the state of the provider. These are defined by the consumer by creating a pact like: 
```scala
  val pact: RequestResponsePact =
  ConsumerPactBuilder
    .consumer("Consumer")
    .hasPactWith("Provider")
    .given("user exists", Map("id" -> "bob")) // provider state id and parameters (optional)
    .uponReceiving(...)
    ...
```

In order to verify pacts that require state, your mock provider server should expose a POST endpoint (e.g. named "setup", or something similar) that expects a request body of `{"state" : "the provider state id string", "params": { "id": "bob" } }` (`params` are optional). Then by setting the field `stateChangeUrl` on the `provider` in your test suite:

```scala
val provider: ProviderInfoBuilder =
  // alternatively: withStateChangeEndpoint("/setup")
  ProviderInfoBuilder().withStateChangeUrl("http://localhost:1234/setup")
```

This will cause a request to be sent to the setup url prior to verification of each interaction that requires provider state. See [our internal test setup here](https://github.com/jbwheatley/pact4s/blob/main/shared/src/test/scala/pact4s/MockProviderServer.scala) for an example of how we handle provider state.

---

## Contributing

We use [sbt-projectmatrix](https://github.com/sbt/sbt-projectmatrix) to easily reuse code across the different scala and jdk versions. Using `sbt test` with `projectmatrix` doesn't seem to respect turning off parallel test execution, which we need because the tests use locking resources. So instead, in order to run the tests use `sbt commitCheck` to run the tests in series. This is quite slow, so `sbt quickCommitCheck` will only run the tests on scala 2.13. Thanks to [sbt-commandmatrix](https://github.com/indoorvivants/sbt-commandmatrix) for enabling this. 

### IntelliJ

When importing the project into the [IntelliJ IDE](https://www.jetbrains.com/idea/), the Scala 2.12.x and Java 8 projects and dependency classpaths will be excluded to prevent IntelliJ from complaining about duplicate classes on the classpath. This means when developing and running tests inside the IDE, Scala 2.13.x and Java 11 will be used.
