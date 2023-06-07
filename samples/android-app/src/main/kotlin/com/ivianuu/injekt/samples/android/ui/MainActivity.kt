/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.ScopedObjects
import com.ivianuu.injekt.samples.android.app.App
import kotlinx.coroutines.job

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val scopedObjects = ScopedObjects<ActivityScope>()
    lifecycleScope.coroutineContext.job.invokeOnCompletion { scopedObjects.dispose() }

    val component = (application as App).appComponent.mainActivityComponent(scopedObjects)
    setContent {
      component.appTheme {
        component.appUi()
      }
    }
  }
}

@Provide data class MainActivityComponent(val appTheme: AppTheme, val appUi: AppUi)

object ActivityScope
