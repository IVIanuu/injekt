# Injekt

Next gen dependency injection library for Kotlin.
```kotlin
@Provide fun jsonParser() = JsonParser()

interface Http

@Provide class HttpImpl : Http

@Provide class Api(private val http: Http, private val jsonParser: JsonParser)

@Provide class Repository(private val api: Api)

@Provide data class AppComponent(val repository: Repository)

val graph = create<AppComponent>()
graph.repo
```

# Setup
TODO
Must be checked out and build locally using publishToMavenLocal task for now

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
  val db = create<Db>()
}
```
Later it should be disposed like so.
```kotlin
fun onDestroy() {
  uiScope.dispose()
}
```

# Multi injection
You can inject all injectables of a given type by injecting a ```List<T>```
```kotlin
@Provide fun singleElement(): String = "a"
@Provide fun multipleElements(): Collection<String> = listOf("b", "c")

fun main() {
  create<List<String>>() == listOf("a", "b", "c") // true
}
```
All elements which match E or Collection\<E\> will be included in the resulting list.

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

// break circular dependency
@Provide class Foo(val bar: Bar)
@Provide class Bar(foo: (Bar) -> Foo) {
   val foo = foo(this)
}
```

# Distinguish between types
Sometimes you have multiple injectables of the same type
Injekt will need help to keep them apart here are two strategies:

Value classes:
```kotlin
@JvmInline value class PlaylistId(val value: String)
@JvmInline value class UserId(val value: String)

@Provide class PlaylistTracksPresenter(playlistId: PlaylistId, userId: UserId)
```

Tags:
```kotlin
@Tag 
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
annotation class PlaylistId
@Tag
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
annotation class UserId

@Provide class PlaylistTracksPresenter(playlistId: @PlaylistId String, userId: @UserId String)
```

Optionally you can add a typealias for your tag to make it easier to use
```kotlin
@Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
annotation class PlaylistIdTag
typealias PlaylistId = @PlaylistIdTag String
@Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
annotation class UserIdTag
typealias UserId = @UserIdTag String

@Provide class PlaylistTracksPresenter(playlistId: PlaylistId, userId: UserId)
```

# Errors / Debugging
Injekt will show an error if there are missing dependencies.
Additionally it will dump kotlin like code with the generated code in the /build/injekt/dump folder
for each file where injections happen

# More complex uses can be found in my essentials project(base project for my apps)
https://github.com/IVIanuu/essentials
