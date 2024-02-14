# Injekt

Next gen dependency injection library for Kotlin.
# Example:
```kotlin
interface Api

@Provide object ApiImpl : Api

interface Repository

@Provide class RepositoryImpl(private val api: Api) : Repository

suspend fun main() {
  val repo = inject<Repository>()
  // do cool things
}
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

# Function injection
Sometimes you want to delay the creation, need multiple instances, want to provide additional parameters,
or to break circular dependencies.
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

// inject functions in a inline function to create a conditional Logger with zero overhead
@Provide inline fun logger(isDebug: IsDebug, loggerImpl: () -> LoggerImpl, noOpLogger: () -> NoOpLogger): Logger =
  if (isDebug) loggerImpl() else noOpLogger()
```

# Multi injection
You can inject all injectables of a given type by injecting a ```List<T>```
```kotlin
@Provide fun singleElement(): String = "a"
@Provide fun multipleElements(): Collection<String> = listOf("b", "c")

fun main() {
  inject<List<String>>() == listOf("a", "b", "c")
}
```
All elements which match the E or Collection\<E\> will be included in the resulting list.

# Scoping
The core of Injekt doesn't know anything about scoping, but there is a api in the common module.
You have to annotate your class or the return type of a function or a property with ```@Scoped``` tag.
```kotlin
@Provide @Scoped<UiScope> class Db
```
Then you have to provide a ```Scope``` instance.
```kotlin
// use a object as name for the scope
object UiScope
```
Then you can inject your class.
```kotlin
@Provide val uiScope = Scope<UiScope>()

fun onCreate() {
  // use ui scoped dependency
  val db = inject<Db>()
}

// dispose scope instance
fun onDestroy() {
  uiScope.dispose()
}
```

# Distinguish between types
Sometimes you have multiple injectables of the same type
Injekt will need help to keep them apart here are two strategies:

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

# Modules
There is no ```@Module``` annotation in Injekt instead a module is just a provided class with
@Provide declarations
```kotlin
// object module which is marked with @Provide
// can be used to organize providers
@Provide object DatabaseModule {
  @Provide fun databaseFile(): File = ...
}

// module with parameters which can be provided later
class NetworkModule(val apiKey: String) {
  @Provide fun api(): Api = ...
}

fun main() {
  @Provide val networkModule = NetworkModule(if (isDebug) ... else ...)
  inject<Api>()
}
```

# Components
There is also no ```@Component``` annotation in Injekt instead a component can be declared
like this without a lot boilerplate
```kotlin
@Provide class ActivityComponent(
  val api: Api,
  val fragmentComponent: (Fragment) -> FragmentComponent
)
```

# Type keys
TODO

# Source keys
TODO

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
  classpath("com.ivianuu.injekt:core:${latest_version}")
  // optional - common utilities
  classpath("com.ivianuu.injekt:common:${latest_version}")
}
```
It's also required to install the Injekt IDE plugin
