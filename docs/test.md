# Injekt: Next gen DI framework powered by a Kotlin compiler plugin

Injekt is a compile-time checked DI framework for Kotlin developers.
Injekt is highly experimental and the api is unstable.

Minimal example:
```kotlin
// provide instances
@Provide val foo = Foo()
@Provide fun bar(foo: Foo) = Bar(foo)

fun main() {
    // inject instance
    val bar = inject<Bar>()
    println("Got $bar")
}
```
