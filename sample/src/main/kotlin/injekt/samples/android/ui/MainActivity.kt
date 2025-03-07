/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.ui

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import injekt.*
import injekt.common.*
import injekt.samples.android.app.*

class MainActivity : ComponentActivity() {
  private val scope = Scope<ActivityScope>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val dependencies = (application as App).appDependencies.mainActivityDependencies(scope)
    setContent {
      dependencies.appTheme.Content {
        dependencies.appUi.Content()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.dispose()
  }
}

@Provide data class MainActivityDependencies(
  val appTheme: AppTheme,
  val appUi: AppUi
)

object ActivityScope
