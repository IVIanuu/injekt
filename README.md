# Injekt: Next gen DI framework powered by a Kotlin compiler plugin

Injekt is a compile-time checked DI framework for Kotlin developers.
Injekt is highly experimental and the api is unstable.

Minimal example:
```kotlin
// provide injectables
@Provide val foo = Foo()
@Provide fun bar(foo: Foo) = Bar(foo)

fun main() {
  // inject
  val bar = inject<Bar>()
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
  // optional - coroutines support
  classpath("com.ivianuu.injekt:injekt-coroutines:${latest_version}")
  // optional - android support
  classpath("com.ivianuu.injekt:injekt-android:${latest_version}")
  // optional - androidx work support
  classpath("com.ivianuu.injekt:injekt-android-work:${latest_version}")
  // optional - compose support
  classpath("com.ivianuu.injekt:injekt-compose:${latest_version}")
  // optional - ktor support
  classpath("com.ivianuu.injekt:injekt-ktor:${latest_version}")
}
```
It's also required to install the Injekt IDE plugin

# Declare givens
TODO

# Use givens
TODO

# Type aliases
TODO

# Qualifiers
TODO

# Sets
TODO

# Providers
TODO

# Scopes
TODO

# Coroutines
TODO

# Android
TODO

# Android work
TODO

# Compose
TODO

# Ktor
TODO

# Type keys
TODO
