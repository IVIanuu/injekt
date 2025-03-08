/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.ui

import androidx.compose.material.*
import androidx.compose.runtime.*
import injekt.*
import injekt.samples.android.domain.*

// declare UiDecorator interface which will be used by MainActivity
// to decorate the UI
// MainActivity will inject a list so we can provide as many as we want
fun interface UiDecorator {
  @Composable fun Content(content: @Composable () -> Unit)

  // mark companion object with @Provide to ensure that providers
  // within its class body will also be seen by injekt.
  // kind of like a module
  @Provide companion object {
    // provide an empty list of UiDecorators to ensure that injekt
    // can provide a list even if no UiDecorators are provided
    @Provide fun defaultUiDecorators(): List<UiDecorator> = emptyList()
  }
}

@Provide val AppColors = darkColors()

@Provide fun appThemeUiDecorator(appColors: Colors) = UiDecorator { content ->
  MaterialTheme(colors = appColors) {
    Surface(content = content)
  }
}

@Provide fun analyticsUiDecorator(analytics: Analytics) = UiDecorator { content ->
  LaunchedEffect(true) {
    analytics.log("Ui Launched")
  }

  content()
}
