/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.ui

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.runtime.*
import injekt.*
import injekt.common.*
import injekt.samples.android.app.*

class MainActivity : ComponentActivity() {
  // declare activity scope here to ensure it has the same lifecycle
  // as this activity instance
  private val scope = Scope<ActivityScope>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // get a reference to the app dependencies
    // and create our own dependencies with our scope
    val dependencies = (application as App)
      .appDependencies
      .mainActivityDependencies(scope, this)

    // combine the list of UiDecorators and set the content
    val decoratedUi: @Composable () -> Unit =
      dependencies.decorators
        .fold({ dependencies.counterUi() }) { content, uiDecorator ->
          { uiDecorator.Content(content) }
        }

    setContent(content = decoratedUi)
  }

  override fun onDestroy() {
    super.onDestroy()
    // properly dispose the scope to ensure that potential
    // ScopeDisposables will also get disposed properly
    scope.dispose()
  }
}

// declare dependencies for MainActivity
@Provide data class MainActivityDependencies(
  // inject a list of all UiDecorators provided in the app
  val decorators: List<UiDecorator>,
  // get a reference to CounterScreen by injecting a @Composable function
  // with the return type of CounterScreen
  val counterUi: @Composable () -> CounterUi
)

data object ActivityScope
