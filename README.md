# Injekt

Next gen dependency injection library for Kotlin

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

fun main() = runBlocking {
  val user = User(1)
  loadUsersById(1) // expands to loadUsersById(1, RepositoryImpl(ApiImpl))
}
```

# Inject injectables
You can automatically inject dependencies into functions and classes 
by marking parameters with @Inject:
```kotlin
// functions
infix operator fun <T> T.compareTo(other: T, @Inject comparator: Comparator<T>) = ...

// classes
class MyService(@Inject private val logger: Logger)

fun main() {
  // automatically injects comparator if provided
  "a" compareTo "b"
  
  // uses explicit arg
  MyService(NoOpLogger)
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

The inject<T>() is declared in the core module and simply defined as follows:
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
```

# Function support
If you want to delay the creation, need multiple instances or if you want to provide additional parameters dynamically.
You can do this by injecting a function.
```kotlin
fun main() {
  
}
```

#Components
TODO

# Scoping

# Multi injection
TODO

# Distinguish between types
Sometimes you have multiple injectables of the same type
Injekt will need help to keep them apart here a are a few strategies:

Type aliases:
```kotlin
typealias PlaylistId = String
typealias TrackId = String

fun loadPlaylistTracks(@Inject playlistId: PlaylistId, @Inject trackId: TrackId): List<Track> = ...
```

Inline classes:
```kotlin
inline class PlaylistId(val value: String)
inline class TrackId(val value: String)

fun loadPlaylistTracks(@Inject playlistId: PlaylistId, @Inject trackId: TrackId): List<Track> = ...
```

Tags:
```kotlin
@Tag annotation class PlaylistId
@Tag annotation class TrackId

fun loadPlaylistTracks(@Inject playlistId: @PlaylistId String, @Inject trackId: @TrackId String): List<Track> = ...
```

# Coroutines
TODO

# Compose

# Android
TODO

# Android work
TODO

# Type keys
TODO

# Source keys
TODO

# Injectable chaining

# Full kotlin support
inline, reified, fun interface lambdas, default parameter value, abstract, expect/actual

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
}
```
It's also required to install the Injekt IDE plugin

