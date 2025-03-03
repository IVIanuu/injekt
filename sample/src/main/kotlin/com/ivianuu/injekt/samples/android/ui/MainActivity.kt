/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.ui

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.samples.android.app.*

class MainActivity : ComponentActivity() {
  private val scope = Scope<ActivityScope>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val component = (application as App).appComponent.mainActivityComponent(scope)
    setContent {
      component.appTheme.Content {
        AppUi()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.dispose()
  }
}

@Provide data class MainActivityComponent(
  val appTheme: AppTheme
)

object ActivityScope
