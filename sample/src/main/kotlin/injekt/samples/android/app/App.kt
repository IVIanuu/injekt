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
  @Provide private val appScope = Scope<AppScope>()
  val appDependencies by lazy { create<AppDependencies>() }

  override fun onCreate() {
    super.onCreate()
    appDependencies.coroutineScope.launch {
      appDependencies.analytics.log("App started")
    }
  }
}

object AppScope

@Provide data class AppDependencies(
  val analytics: Analytics,
  val coroutineScope: ScopedCoroutineScope<AppScope>,
  val mainActivityDependencies: (Scope<ActivityScope>) -> MainActivityDependencies
)
