/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.app

import android.app.Application
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scope
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.samples.android.ui.ActivityScope
import com.ivianuu.injekt.samples.android.ui.MainActivityComponent
import com.ivianuu.injekt.injectContext

class App : Application() {
  val appComponent by lazy {
    inject<_, AppComponent>(Scope<AppScope>()) { inject() }
  }
}

object AppScope

@Provide data class AppComponent(val mainActivityComponent: (Scope<ActivityScope>) -> MainActivityComponent)
