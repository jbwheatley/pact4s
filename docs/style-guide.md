# Style guide

## Motivators

The north star to be kept in mind when developing this library is to **reduce the friction of adding pact testing to a Scala project**.
This manifests in a number of guiding principles that should be followed:

1. Keep dependencies to a minimum - clashes between library versions are an inevitable part of building software, so we should strive not
   to contribute to this pain-point.

2. Make the library widely available - `pact4s` is built for multiple Scala versions, Java versions, and Scala testing framework.

3. Compatibility between versions - as this is a testing library, it is unlikely that users will face issues with diamond dependencies. So, while
   we can allow ourselves a little more wiggle room when it comes to compatibility between versions, we want to reduce the overhead of bumping
   versions of this library as much as possible.

4. Use functional programming practices where it makes sense - it is expected that a large majority of Scala users will be writing projects in a (more)
   functional style, so we should aim to do the same. However, there are places where full functional style wouldn't be appropriate, for example capturing
   the side effects of the underlying `pact-jvm` in a effectful monad (e.g. cats-effect's IO monad). Adding a dependency on an FP library would be antithetical to
   points 1 and 2 above, so some leeway in terms of functional style should be expected.

5. Make it easy to add new framework modules - as much code should be reused as possible in the shape of a shared API between testing frameworks to ensure that
   adding compatibility with other testing libraries can be done quickly.

## Code guidelines

This style guide will be opinionated (it is about style, after all), but the consistency of the code contributed to this library will help enable the above
principle to be realised faster, and hopefully with fewer bugs. Much of this library is concerned with removing the need for users to directly use Java/Kotlin in
their Scala code.

### Extension methods

Extension methods that eliminate the need for users to use Java data types should be available without the need of "magic" imports wherever a user is writing pact-related tests. They should be mixed in to the base test traits, and formatted as follows:

```scala
private[pact4s] object FooOps {
	class FooExtension(val foo: Foo) extends AnyVal {
		def bar(bar: Bar): Foo = ???
	}
}

trait FooOps {
	implicit def toFooExtension(val foo: Foo): FooExtension = new FooExtension(foo)
}
```

See [an example here](../shared/src/main/scala/pact4s/syntax/RequestResponsePactOps.scala) and it [mixed in here](../shared/src/main/scala/pact4s/RequestResponsePactForgerResources.scala).

### Builder pattern for data classes

Scala counterparts should be provided for `pact-jvm` models that are user-facing - these mainly consist of the classes used to define the provider on the verification side. To construct these data types, we prefer to use a builder pattern, rather than using a case class. For example:

```scala
final class Foo private (a: String, b: Int, c: Option[Boolean], d: Option[Double]) {
	private def copy(a: String = a, b: Int = b, c: Option[Boolean] = c, d: Option[Double] = d) = new Foo(a, b, c, d)

	def withC(cc: Boolean) = copy(c = Some(cc))
	def withD(dd: Double)  = copy(d = Some(dd))
}

object Foo {
	def apply(a: String, b: Int): Foo = new Foo(a, b, None, None)
}
```

See [an example here](../models/src/main/scala/pact4s/provider/ProviderInfoBuilder.scala).

This is preferred as it allows fields to be added and deprecated easily and with minimal compatibility issues (e.g. due to additional case class fields), and allows us to easily disallow certain invalid combinations of parameters from being constructed.

### Miscellanea

- Members of a sealed trait should be in the companion object as this aids discovery.

- All data classes should be `final`

- Only classes and methods that are required by the user should be public. Think carefully about whether something should be public, private, or protected.

- Basic data types should be wrapped to avoid confusion and misuse - e.g. `final case class Branch(value: String)` rather than simply `String`.

- Almost everything should have a scaladoc.

- Exceptions should only be thrown at the edge of the program, e.g. in the framework implementations.

- Package declarations should each subdirectory on separate lines - `package pact4s \n package syntax`, rather than `package pact4s.syntax` (I don't remember why I did it this way, but it's better to be consistent now)