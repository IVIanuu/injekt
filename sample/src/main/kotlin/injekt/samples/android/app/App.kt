/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.app

import android.app.*
import injekt.Provide
import injekt.common.Scope
import injekt.inject
import injekt.samples.android.ui.*

class App : Application() {
  @Provide private val appScope = Scope<AppScope>()
  val appComponent by lazy { inject<AppComponent>() }
}

object AppScope

@Provide data class AppComponent(
  val mainActivityComponent: (Scope<ActivityScope>) -> MainActivityComponent
)
