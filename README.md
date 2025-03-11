# Injekt

Next gen dependency injection library for Kotlin.
```kotlin
@Provide fun jsonParser() = JsonParser()

interface Http

@Provide class HttpImpl : Http

@Provide class Api(private val http: Http, private val jsonParser: JsonParser)

@Provide class Repository(private val api: Api)

@Provide data class AppDependencies(val repository: Repository)

fun main() {
  val dependencies = create<AppDependencies>() // translates to AppDependencies(Repository(Api(HttpImpl(), jsonParser()))
  dependencies.repo
}
```

# Setup
```kotlin
plugins {
  id("io.github.ivianuu.injekt") version latest_version
}

dependencies {
  // core runtime
  implementation("io.github.ivianuu.injekt:core:${latest_version}")
  // optional - common utilities
  implementation("io.github.ivianuu.injekt:common:${latest_version}")
}
```

# Providers
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

# Scoping
The core of Injekt doesn't know anything about scoping, but there is a api in the common module.
You have to annotate your class or the return type of a function or a property with ```@Scoped``` tag.
```kotlin
@Provide @Scoped<UiScope> class MyViewModel
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
  val db = create<MyViewModel>()
}
```
Later it should be disposed like so.
```kotlin
fun onDestroy() {
  uiScope.dispose()
}
```

# Multi injection
You can inject all dependencies of a given type by injecting a ```List<T>```
```kotlin
@Provide fun singleElement(): String = "a"
@Provide fun multipleElements(): Collection<String> = listOf("b", "c")

fun main() {
  create<List<String>>() == listOf("a", "b", "c") // true
}
```

# Function injection
Sometimes you want to delay the creation, need multiple instances, want to provide additional parameters,
or to break circular dependencies.
You can do this by injecting a function.
```kotlin
// inject a function to create multiple Tokens
@Provide class HttpClient(tokenFactory: () -> Token) {
  val tokenA = tokenFactory()
  val tokenB = tokenFactory()
}

// inject a function to create a MyViewModel with the additional String parameter
@Provide class MyActivity(viewModelFactory: (String) -> MyViewModel) {
  val viewModel by lazy { viewModelFactory("user_id") }
}

// break cycles
@Provide class Foo(val bar: Bar)
@Provide class Bar(foo: (Bar) -> Foo) {
   val foo = foo(this)
}
```

# Distinguish between types
Sometimes you have multiple dependencies of the same type
Injekt will need help to keep them apart here are three strategies:
```kotlin
// value class
@JvmInline value class PlaylistId(val value: String)
@Provide val playlistId = PlaylistId("my_playlist")

// tag annotation
@Tag annotation class CurrentUserId
@Provide val currentUserId: @CurrentUserId String = "my_user_id"

// tag typealias
@Tag typealias PlaylistOwnerId = String
@Provide val playlistOwnerId: PlaylistOwnerId = "my_playlist_owner_id"

@Provide class PlaylistTracksPresenter(
  playlistId: PlaylistId, // = PlaylistId("my_playlist")
  currentUserId: @CurrentUserId String, // "my_user_id"
  playlistOwnerId: PlaylistOwnerId // "my_playlist_owner_id"
)
```

# Errors / Debugging
Injekt will show an error if there are missing dependencies.
Additionally it will dump generated code in a kotlin like syntax in the /build/injekt/dump folder
for each file where injections happen

# More complex uses can be found in my essentials project(base project for my apps)
https://github.com/IVIanuu/essentials
