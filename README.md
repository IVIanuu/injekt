# Injekt: Next gen DI framework for Kotlin



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
by marking parameters with @Inject
```kotlin
// functions
infix operator fun <T> T.compareTo(other: T, @Inject comparator: Comparator<T>) = ...

// classes
class MyService(@Inject private val logger: Logger)
```

Injekt will then try to resolve the dependencies on each call site if no explicit argument was provided

# Provide injectables
You can provide dependencies by annotating them with @Provide
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
@Provide val apiKey: ApiKey = ""
```

# Distinguish between types

Sometimes you have multiple instances of the same 
```kotlin

```

# Functions
TODO

# Components
TODO

# Scoping

# Lists
TODO

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
inline, reified, fun interface lambdas, default parameter value

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

