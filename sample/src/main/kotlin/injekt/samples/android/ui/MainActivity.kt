/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.ui

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import injekt.samples.android.app.*
import injekt.Provide
import injekt.common.Scope

class MainActivity : ComponentActivity() {
  private val scope = Scope<ActivityScope>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val component = (application as App).appComponent.mainActivityComponent(scope)
    setContent {
      component.appTheme.Content {
        component.appUi.Content()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.dispose()
  }
}

@Provide data class MainActivityComponent(val appTheme: AppTheme, val appUi: AppUi)

object ActivityScope
