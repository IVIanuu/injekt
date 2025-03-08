/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.app

import android.app.*
import injekt.*
import injekt.common.*
import injekt.samples.android.domain.*
import injekt.samples.android.ui.*
import injekt.samples.android.util.*
import kotlinx.coroutines.*

class App : Application() {
  // store the app scope here to allow @Scoped<AppScope> instances
  // and provide it so that the create call below can use it
  @Provide private val appScope = Scope<AppScope>()
  // lazily create an instance of AppDependencies by calling injekt's create() function
  // injekt uses all declared providers to create them
  val appDependencies by lazy { create<AppDependencies>() }

  override fun onCreate() {
    super.onCreate()
    appDependencies.coroutineScope.launch {
      appDependencies.analytics.log("App started")
    }
  }
}

// name for our AppScope
object AppScope

// declare dependencies used in our App
@Provide data class AppDependencies(
  val analytics: Analytics,
  val coroutineScope: ScopedCoroutineScope<AppScope>,
  // add a factory for activity dependencies to allow it to have its own scope
  // this creates a hierarchy of scopes similar to subcomponents in other di libs
  val mainActivityDependencies: (Scope<ActivityScope>) -> MainActivityDependencies
)
