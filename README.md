# Injekt: Next gen DI framework powered by a Kotlin compiler plugin

Injekt is a compile-time checked DI framework for Kotlin developers.
Injekt is highly experimental and the api is unstable.

Minimal example:
```kotlin
// Declares a given value.
@Given val foo = Foo()

fun main() {
    // retrieve a Foo instance
    val foo = given<Foo>()
    println("Got $foo")
}
```

# Request dependencies
TODO

# Declare dependencies
TODO

# Given set elements
TODO

# GivenFuns
TODO

# Macros
TODO

# Interceptors
TODO

# Modules
TODO

# Qualifiers
TODO

# Typealiases
TODO

# Providers
TODO

# Scopes
TODO

# Keys
TODO

# Components
TODO

# Android
TODO

# Android work
TODO
