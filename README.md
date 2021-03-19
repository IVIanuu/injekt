# Injekt: Next gen DI framework powered by a Kotlin compiler plugin

Injekt is a compile-time checked DI framework for Kotlin developers.
Injekt is highly experimental and the api is unstable.

Minimal example:
```kotlin
// declare givens
@Given val foo = Foo()
@Given fun bar(@Given foo: Foo) = Bar(foo)

fun main() {
    // retrieve instance
    val bar = given<Bar>()
    println("Got $bar")
}
```

# Request dependencies
TODO

# Declare dependencies
TODO

# Sets
TODO

# Qualifiers
TODO

# Type aliases
TODO

# Providers
TODO

# Given scopes
TODO

# Android
TODO

# Android work
TODO

# Type keys
TODO
