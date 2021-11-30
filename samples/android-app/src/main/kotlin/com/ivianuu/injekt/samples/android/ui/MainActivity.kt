/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.ui

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.lifecycle.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.samples.android.app.*
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val scope = Scope<ActivityScope>()
    lifecycleScope.coroutineContext.job.invokeOnCompletion { scope.dispose() }

    val component = (application as App).appComponent.mainActivityComponent(scope)
    setContent {
      component.appTheme {
        component.appUi()
      }
    }
  }
}

@Provide data class MainActivityComponent(val appTheme: AppTheme, val appUi: AppUi)

object ActivityScope
