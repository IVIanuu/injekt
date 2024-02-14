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

class App : Application() {
  @Provide private val appScope = Scope<AppScope>()
  val appComponent by lazy { inject<AppComponent>() }

  /* generated
  val appComponent by lazy {
    inject(
      x = AppComponent(mainActivityComponent = { activityScope ->
        MainActivityComponent(
          appTheme = AppTheme.impl,
          appUi = AppUi.impl(
            presenter = CounterPresenter.impl(
              db = Scoped.scoped(
                scope = appScope,
                key = TypeKey(value = "com.ivianuu.injekt.samples.android.data.CounterDbImpl"),
                init = { CounterDbImpl() }
              )
            )
          )
        )
      }
    ))
  }*/
}

object AppScope

@Provide data class AppComponent(val mainActivityComponent: (Scope<ActivityScope>) -> MainActivityComponent)
