/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.app

import android.app.Application
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.ScopedObjects
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.samples.android.ui.ActivityScope
import com.ivianuu.injekt.samples.android.ui.MainActivityContext

class App : Application() {
  @Provide private val scopedObjects = ScopedObjects<AppScope>()
  val appContext by lazy { inject<AppContext>() }
}

object AppScope

@Provide data class AppContext(val mainActivityContext: (ScopedObjects<ActivityScope>) -> MainActivityContext)
