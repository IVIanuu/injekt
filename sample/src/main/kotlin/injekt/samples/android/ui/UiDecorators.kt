/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.ui

import androidx.compose.material.*
import androidx.compose.runtime.*
import injekt.*
import injekt.samples.android.domain.*

fun interface UiDecorator {
  @Composable fun Content(content: @Composable () -> Unit)
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
