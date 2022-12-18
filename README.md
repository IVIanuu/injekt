# Injekt

Next gen dependency injection library for Kotlin.
# Example:
```kotlin
data class User(val id: Long)

interface Api

@Provide object ApiImpl : Api

interface Repository {
  suspend fun loadAll(): List<User>
}

@Provide class RepositoryImpl(private val api: Api) : Repository {
  ...
}

suspend fun loadUserById(id: Long, @Inject repository: Repository): User? = ...

suspend fun main() {
  val user = User(1)
  loadUsersById(1) // expands to loadUsersById(1, RepositoryImpl(ApiImpl))
}
```

# Inject injectables
You can automatically inject dependencies into functions and classes 
by marking parameters with @Inject:
```kotlin
// functions
fun <T> T.compareTo(other: T, @Inject comparator: Comparator<T>) = ...

// classes
class MyService(@Inject private val logger: Logger)

fun main() {
  // automatically injects provided comparator
  "a".compareTo("b")
  
  // uses explicit arg
  MyService(NoOpLogger).run()
}
```

Injekt will then try to resolve the dependencies on each call site if no explicit argument was provided.

You can also inject a injectable at any point with the inject<T>() function:
```kotlin
fun main() {
  val logger = inject<Logger>()
  logger.log(message)
}
```

The inject<T>() function is declared in the core module and simply defined as follows:
```kotlin
inline fun <T> inject(@Inject value: T) = value
```

# Provide injectables
You can provide dependencies by annotating them with @Provide:
```kotlin
// classes and objects
@Provide class MyApi(baseUrl: BaseUrl)

// constructors
class MyService @Provide constructor(logger: Logger) {
  @Provide constructor()
}

// functions
@Provide fun okHttp(authenticator: Authenticator): OkHttpClient = ...

// properties and local variables
@Provide val apiKey: ApiKey = ...

// value parameters
fun run(@Provide config: Config) {
}
```

You can also declare ```suspend``` and ```@Composable``` provide functions.

# How injectables will be resolved
1. Injekt looks at all provided injectables in the current scope 
e.g. enclosing local variables, function parameters, classes, injectables in the current package and so on
and chooses the closest most specific one.
```kotlin
suspend fun main() {
  @Provide val dispatcher: IoDispatcher = ...
  withContext(inject<CoroutineDispatcher>()) {
  }
}
```

2. Injekt will also consider declarations imported with the ```@Providers(...)```.
The ```@Providers``` can be placed anywhere in a file and will only affect the nested scope.
```kotlin
// file wide imports
@file:Providers("injectables.*")

package mypackage

// class wide imports
@Providers("network.Api") 
class MyClass {
  // function wide imports
  @Providers("domain.*")
  fun main() {
    // expression wide imports
    @Providers("data.*") 
    runApp()
  }
}
```

3. If no injectable was found injekt will look into the package of the injected type and also in 
   the packages of all of it's arguments and super types.

Provider imports are only required if the injectable is not in the current scope 
or in a package of the injected type

# Function injection
Sometimes you want to delay the creation, need multiple instances, want to provide additional parameters dynamically,
break circular dependencies or you aren't in the right call context yet.
You can do this by injecting a function.
```kotlin
// inject a function to create multiple Tokens
fun run(@Inject tokenFactory: () -> Token) {
  val tokenA = tokenFactory()
  val tokenB = tokenFactory()
}

// inject a function to create a MyViewModel with the additional String parameter
@Composable fun MyScreen(@Inject viewModelFactory: (String) -> MyViewModel) {
  val viewModel = remember { viewModelFactory("user_id") }
}

// break circular dependency
@Provide class Foo(val bar: Bar)
@Provide class Bar(foo: (Bar) -> Foo) {
   val foo = foo(this)
}

// inject a function to create a dependency in suspend context
fun startService(@Inject dbFactory: suspend () -> Db) {
  scope.launch {
    val db = dbFactory()
  }
}

// inject functions in a inline function to create a conditional Logger with zero overhead
@Provide inline fun logger(isDebug: IsDebug, loggerImpl: () -> LoggerImpl, noOpLogger: () -> NoOpLogger): Logger =
  if (isDebug) loggerImpl() else noOpLogger()
```
You can also inject ```suspend``` and ```@Composable``` functions.

# Multi injection
You can inject all injectables of a given type by injecting a ```List<T>```
```kotlin
@Provide fun singleElement(): String = "a"
@Provide fun multipleElements(): Collection<String> = listOf("b", "c")

fun main() {
  inject<List<String>>() == listOf("a", "b", "c")
}
```
All elements which match the T or Collection\<T\> will be included in the resulting list.

# Scoping
TODO

# Elements api
TODO

# Distinguish between types
Sometimes you have multiple injectables of the same type
Injekt will need help to keep them apart here a are a two strategies:

Value classes:
```kotlin
@JvmInline value class PlaylistId(val value: String)
@JvmInline value class TrackId(val value: String)

fun loadPlaylistTracks(@Inject playlistId: PlaylistId, @Inject trackId: TrackId): List<Track> = ...
```

Tags:
```kotlin
@Tag annotation class PlaylistId
@Tag annotation class TrackId

fun loadPlaylistTracks(@Inject playlistId: @PlaylistId String, @Inject trackId: @TrackId String): List<Track> = ...
```

Optionally you can add a typealias for your tag to make it easier to use
```kotlin
@Tag annotation class PlaylistIdTag
typealias PlaylistId = @PlaylistIdTag String
@Tag annotation class TrackIdTag
typealias TrackId = @TrackIdTag String

fun loadPlaylistTracks(@Inject playlistId: PlaylistId, @Inject trackId: TrackId): List<Track> = ...
```

# Injectable chaining
TODO

# Coroutines
TODO

# Compose
TODO

# Android
TODO

# Android work
TODO

# Type keys
TODO

# Source keys
TODO

# Full kotlin support
inline, reified, fun interface, lambdas, default parameter value, abstract, expect/actual

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
  // optional - coroutines utilities
  classpath("com.ivianuu.injekt:injekt-coroutines:${latest_version}")
  // optional - android utilities
  classpath("com.ivianuu.injekt:injekt-android:${latest_version}")
  // optional - androidx work utilities
  classpath("com.ivianuu.injekt:injekt-android-work:${latest_version}")
}
```
It's also required to install the Injekt IDE plugin
