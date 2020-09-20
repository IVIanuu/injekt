# Injekt: Next gen DI framework powered by a Kotlin compiler plugin

Minimal example:
```kotlin
// Declares a given instance.
@Given fun foo() = Foo()

@InitializeInjekt // Trigger generation of context implementations in the current compilation unit.
class MyApplication : Application() {
    override fun onCreate() {
        // run a reader function and retrieve a instance of Foo
        val foo = applicationReaderContext.runReader { given<Foo>() }
    }
}
```

# Reader functions
Injekt has the concept of reader functions.
These functions are allowed to inject givens via calls to the given<T>() function.
Reader functions can be declared by annotating a function with '@Reader'.

```kotlin
@Reader
fun greet() {
    val logger = given<Logger>()
    logger.log("hello world")
}
```

# Reader classes
Similar to reader functions there are also reader classes.

```kotlin
@Reader
class Greeter {
    fun greet() {
        val logger = given<Logger>()
        logger.log("hello world")
    }
}
```

# Run reader
Reader functions can only be called from inside other reader functions or classes.
To get into a reader function we can call `runReader` on any context object.
Any call to given<T>() inside the reader block will be retrieved from the receiver context of the `runReader` call.

# Contexts
The context is the object graph where reader functions get their givens from.
Contexts can be instantiated by calling `rootContext`.
`rootContext` can take inputs which can be retrieved in reader functions

```kotlin
class MyApplication : Application() {
    val readerContext = rootContext<ApplicationContext>(this)
}
```

# Givens
Declaring givens is as simple as declaring a function and annotate it with ´@Given´
Given functions are treated like reader functions, so they can also call given<T>() and other reader functions
to build their return value.
Classes can also be marked with ´@Given´
```kotlin
@Given
fun bar() = Bar(given<Foo>())
```

# Initialize injekt
To trigger the generation of context implementations 1 declaration in the compilation unit must
be annotated with `@InitializeInjekt`.
This should be done in the application module.

# Child contexts
Contexts can inherit parent contexts.
This allows to create a context hierarchy where child contexts have a shorter life time than a parent one.
To declare a child context the `childContext` function must be called.
Note that `childContext` is a reader function.
```kotlin
@Given
class MyScreen : Screen() {
    val screenContext = childContext<ScreenContext>(this)
}
```

# Scoped givens
Givens can be scoped to a context.
This means that only 1 instance of the given will be created trough out the lifetime of the associated context.
```kotlin
@Given(SingletonContext::class)
class MySingletonRepository
```

# Given set elements
TODO

# Given map entries
TODO

# Effects
TODO

# Assisted parameters
TODO
