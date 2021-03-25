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

# Setup
```kotlin
// in your buildscript
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("com.ivianuu.injekt:injekt-gradle-plugin:${latest_version}")
  }
}

// in your app module
plugins {
    apply("com.ivianuu.injekt")
}

dependencies {
    // core runtime
    classpath("com.ivianuu.injekt:injekt-core:${latest_version}")
    // optional - common utilities
    classpath("com.ivianuu.injekt:injekt-common:${latest_version}")
    // optional - scope runtime
    classpath("com.ivianuu.injekt:injekt-scope:${latest_version}")
    // optional - android support
    classpath("com.ivianuu.injekt:injekt-android:${latest_version}")
    // optional - androidx work support
    classpath("com.ivianuu.injekt:injekt-android-work:${latest_version}")
}
```
It's also required to install the Injekt IDE plugin

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
