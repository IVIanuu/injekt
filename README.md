# Injekt: Next gen DI framework powered by a Kotlin compiler plugin

Minimal example:
```kotlin
@InitializeInjekt interface InjektInitializer
@Given fun foo(): Foo = Foo()

fun main() {
    runReader { 
        given<Foo>()
    }
}
```

What happens there:
- `@InitializeInject` indicates that this module is supported and records the file where initialization happens.
- Most of the magic happens with the functions marked with `@Reader` annotation. They have an additional context generated for each call of of `given` inside.
- `@Given` indicates a declaration, result of which will be used in the reader graph.
  - `@Given` functions are automatically `@Reader`, i.e. their parameters participate in the graph resolution.
  - Nullable `given` can be provided by non-nullable binding.
  - Same types can be distinguished with a typealias. (`ApplicationContext` - `ActivityContext`) in Android.
  - `given` calls inside this and other `@Reader` functions will be replaced with a method calls on an interface.
- `runReader` is a way to transition between `@Reader` and non-`@Reader` worlds. It will take the scope it is applied on and generate the context to run the function. 

## Scoping
Lifetime scopes are defined as follows:
```
class SingletonStorage : Storage by Storage()

@Given
val provideSingletonStorage = SingletonStorage()

@Given(SingletonStorage::class)
class MySingleton
```

- `Storage` subclass (`SingletonStorage` in this case) provides an identifier of that scope.
  - It also indicates that for that type compiler needs to generate keys.
- `fun scope` in conjuction with `Storage` helper make the scoping happen. Key is generated uniquely for each type / unique instance.

Multi instance scoping can be handled using global storage based on unique elements:
```kotlin
@Scoping
object ActivityScoping {
    private val storages = hashMapOf<Activity, Storage>()

    @Reader
    fun <T> scope(key: Any, init: () -> T) = storage.getOrPut(given<Activity>(), Storage()).scope(key, init)
}

You can also do something like that for custom scopes:
```
class User

class UserStorage : Storage by Storage()

class MyUserStorageHolder {
    
    private var userStorage: UserStorage? = null
    private var user: User? = null
    
    fun login(user: String) {
        this.user = user
        this.userStorage = UserStorage()
    }
    
    @Reader
    fun <R> inUserScope(block: @Reader () -> R): R = runChildReader(
        userStorage!!, user, block = block
    )
    
}

@Given(UserStorage::class)
class UserRepo {
    private val user = given<User>()
}
```

## Effects 

`@Effect` provides additional definitions for types:
```kotlin
typealias FooFactory = () -> Foo
        
@Effect
annotation class BindFooFactory {
    companion object {
        @Given
        operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
    }
}

@BindFooFactory
fun fooFactory(): Foo {
    return Foo()
}

fun invoke(): Foo { 
    return runReader { given<FooFactory>()() }
}
```
The example above contains
  - definition, `annotation class BindFooFactory`, annotated with `Effect`
  - its companion object, which defines types which it provide using bound on type parameter `T`

Annotating function with `@BindFooFactory` will now return reference to this function as in current scope.
So `runReader { given<FooFactory>()() }` will be essentially converted to `runReader { fooFactory() }`.

One more example of using it is to bind different types:
```kotlin
@Effect
annotation class ToString {
    companion object {
        @Given
        operator fun <T : Any> invoke(): String = given<T>().toString()
    }
}

@ToString
@Given
fun foo(): Foo = Foo()

fun invoke(): Foo { 
    return runReader { given<String>() /* Foo().toString() */ }
}
```
